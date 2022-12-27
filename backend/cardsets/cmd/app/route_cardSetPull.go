package main

import (
	"context"
	"models/apphelpers"
	"models/cardset"
	"net/http"
	"time"

	mapset "github.com/deckarep/golang-set/v2"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

type CardSetPullInput struct {
	AccessToken       string   `json:"accessToken"`
	CurrentCardSetIds []string `json:"currentCardSetIds"`
}

func (input *CardSetPullInput) GetAccessToken() string {
	return input.AccessToken
}

func (input *CardSetPullInput) GetRefreshToken() *string {
	return nil
}

type CardSetPullResponse struct {
	UpdatedCardSets   []*cardset.ApiCardSet `json:"cardSetIds,omitempty"`
	DeletedCardSetIds []string              `json:"deletedCardSetIds"`
}

func (app *application) cardSetPull(w http.ResponseWriter, r *http.Request) {

	input, authToken, validateSessionErr := app.pullSessionValidator.Validate(r)
	if validateSessionErr != nil {
		app.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode)
		return
	}

	var lastModificationDate *time.Time
	if parsedDate, err := time.Parse(time.RFC3339, r.URL.Query().Get(ParameterLatestCardSetModificationDate)); err == nil {
		lastModificationDate = &parsedDate
	}

	ctx := r.Context()

	apiCardSets, err := app.cardSetRepository.ModifiedCardSetsSince(ctx, authToken.UserMongoId, lastModificationDate)
	if err != nil {
		app.SetError(w, err, http.StatusInternalServerError)
		return
	}

	idsToDelete, handlerErr := app.resolveDeletedCardIds(ctx, authToken.UserMongoId, input)
	if handlerErr != nil {
		app.SetHandlerError(w, handlerErr)
		return
	}

	response := CardSetPullResponse{
		UpdatedCardSets:   apiCardSets,
		DeletedCardSetIds: idsToDelete,
	}
	app.WriteResponse(w, response)
}

func (app *application) resolveDeletedCardIds(
	ctx context.Context,
	userId *primitive.ObjectID,
	input *CardSetPullInput,
) ([]string, *apphelpers.HandlerError) {
	userCardSetIds, err := app.cardSetRepository.CardCardSetIds(ctx, userId)
	if err != nil {
		return nil, app.NewHandlerError(http.StatusInternalServerError, err)
	}

	currentCardSetIdSet := mapset.NewSet(input.CurrentCardSetIds...)
	for i := range userCardSetIds {
		currentCardSetIdSet.Remove(userCardSetIds[i])
	}

	return currentCardSetIdSet.ToSlice(), nil
}
