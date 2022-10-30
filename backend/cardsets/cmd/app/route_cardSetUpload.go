package main

import (
	"encoding/json"
	"models/apphelpers"
	"models/cardset"
	"models/userauthtoken"
	"net/http"
)

type CardSetUploadInput struct {
	cardSets []cardset.CardSet `json:"cardSets"`
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
	session, err := r.Cookie(apphelpers.CookieSession)
	if err != nil {
		apphelpers.SetError(w, err, app.logger)
		app.clientError(w, http.StatusBadRequest)
		return
	}
	if len(session.Value) == 0 {
		app.clientError(w, http.StatusBadRequest)
		return
	}

	// Header params
	var deviceId = r.Header.Get(app.HeaderDeviceId)
	if len(deviceId) == 0 {
		app.clientError(w, http.StatusBadRequest)
		return
	}

	// Body params
	var input app.RefreshInput
	err = json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		app.clientError(w, http.StatusBadRequest)
		return
	}

	userAuthToken, err := userauthtoken.Load(r.Context(), app.sessionManager)
	if err != nil {
		app.serverError(w, err)
		return
	}

	if !userAuthToken.IsValid() {
		app.clientError(w, http.StatusUnauthorized)
		return
	}

	if !userAuthToken.IsMatched(
		input.AccessToken,
		input.RefreshToken,
		deviceId,
	) {
		app.clientError(w, http.StatusBadRequest)
		return
	}
}
