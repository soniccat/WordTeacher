package main

import (
	"errors"
	"fmt"
	"models/apphelpers"
	"models/cardset"
	"models/user"
	"net/http"
	"time"
)

type CardSetPullInput struct {
	AccessToken string `json:"accessToken"`
}

func (input *CardSetPullInput) GetAccessToken() string {
	return input.AccessToken
}

func (input *CardSetPullInput) GetRefreshToken() *string {
	return nil
}

type CardSetPullResponse struct {
	UpdatedCardSets []*cardset.CardSetApi `json:"cardSetIds,omitempty"` // updated card sets
	DeletedCardSets []string              `json:"deletedCardSets"`
}

func (app *application) cardSetPull(w http.ResponseWriter, r *http.Request) {
	input, authToken, validateSessionErr := user.ValidateSession[CardSetPullInput](r, app.sessionManager)
	if validateSessionErr != nil {
		apphelpers.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode, app.logger)
		return
	}

	query := r.URL.Query()
	if !query.Has(ParameterLastPullDate) {
		apphelpers.SetError(w, errors.New(fmt.Sprintf("%s is missing", ParameterLastPullDate)), http.StatusBadRequest, app.logger)
		return
	}

	var lastPullDate *time.Time = nil
	if parsedDate, err := time.Parse(time.RFC3339, r.URL.Query().Get(ParameterLastPullDate)); err != nil {
		lastPullDate = &parsedDate
	}

}
