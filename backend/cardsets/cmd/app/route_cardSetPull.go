package main

import (
	"context"
	"errors"
	"fmt"
	"github.com/deckarep/golang-set/v2"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/apphelpers"
	"models/cardset"
	"models/user"
	"net/http"
	"time"
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
	UpdatedCardSets   []*cardset.CardSetApi `json:"cardSetIds,omitempty"`
	DeletedCardSetIds []string              `json:"deletedCardSetIds"`
}

func (app *application) cardSetPull(w http.ResponseWriter, r *http.Request) {
	input, authToken, validateSessionErr := user.ValidateSession[CardSetPullInput](r, app.sessionManager)
	if validateSessionErr != nil {
		app.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode)
		return
	}

	query := r.URL.Query()
	if !query.Has(ParameterLatestCardSetModifiedDate) {
		app.SetError(w, errors.New(fmt.Sprintf("%s is missing", ParameterLatestCardSetModifiedDate)), http.StatusBadRequest)
		return
	}

	var lastPullDate *time.Time = nil
	if parsedDate, err := time.Parse(time.RFC3339, r.URL.Query().Get(ParameterLatestCardSetModifiedDate)); err != nil {
		lastPullDate = &parsedDate
	}

	ctx := r.Context()

	app.cardSetRepository.FindCardSetByCreationId()

	idsToDelete, handlerErr := app.resolveDeletedCardIds(ctx, authToken.UserMongoId, input)
	if handlerErr != nil {
		app.SetHandlerError(w, handlerErr)
		return
	}

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
