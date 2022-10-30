package main

import (
	"models/apphelpers"
	"models/cardset"
	"models/user"
	"net/http"
)

type CardSetUploadInput struct {
	AccessToken string               `json:"accessToken,omitempty"`
	CardSets    []cardset.CardSetApi `json:"cardSets"`
}

func (i CardSetUploadInput) GetAccessToken() string {
	return i.AccessToken
}

func (i CardSetUploadInput) GetRefreshToken() *string {
	return nil
}

type CardSetUploadResponse struct {
}

// Purpose:
//
// In:
//
//	Header: deviceId
//	Body: RefreshInput
//
// Out:
//
//	RefreshResponse
func (app *application) cardSetUpload(w http.ResponseWriter, r *http.Request) {

	input, err := user.ValidateSession[CardSetUploadInput](r, app.sessionManager)
	if err != nil {
		apphelpers.SetError(w, err.InnerError, err.StatusCode, app.logger)
		return
	}

}
