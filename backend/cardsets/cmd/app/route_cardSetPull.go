package main

import (
	"context"
	"encoding/json"
	"errors"
	"models/apphelpers"
	"models/cardset"
	"net/http"
	"time"

	mapset "github.com/deckarep/golang-set/v2"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

type CardSetPullInput struct {
	CurrentCardSetIds []string `json:"currentCardSetIds,omitempty"`
}

type CardSetPullResponse struct {
	UpdatedCardSets   []*cardset.ApiCardSet `json:"updatedCardSets,omitempty"`
	DeletedCardSetIds []string              `json:"deletedCardSetIds,omitempty"`
}

func (app *application) cardSetPull(w http.ResponseWriter, r *http.Request) {
	authToken, validateSessionErr := app.sessionValidator.Validate(r)
	if validateSessionErr != nil {
		app.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode)
		return
	}

	if r.Body == nil {
		app.SetError(w, errors.New("body is empty"), http.StatusBadRequest)
		return
	}

	var input CardSetPullInput
	err := json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		app.SetError(w, err, http.StatusBadRequest)
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

	idsToDelete, handlerErr := app.resolveDeletedCardIds(ctx, authToken.UserMongoId, &input)
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
