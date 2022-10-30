package main

import (
	"encoding/json"
	"models/apphelpers"
	"models/userauthtoken"
	"net/http"
)

type RefreshInput struct {
	AccessToken  string `json:"accessToken,omitempty"`
	RefreshToken string `json:"refreshToken,omitempty"`
}

type RefreshResponse struct {
	AccessToken  string `json:"accessToken,omitempty"`
	RefreshToken string `json:"refreshToken,omitempty"`
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
func (app *application) refresh(w http.ResponseWriter, r *http.Request) {
	session, err := r.Cookie(apphelpers.CookieSession)
	if err != nil {
		app.clientError(w, http.StatusBadRequest)
		return
	}
	if len(session.Value) == 0 {
		app.clientError(w, http.StatusBadRequest)
		return
	}

	// Header params
	var deviceId = r.Header.Get(apphelpers.HeaderDeviceId)
	if len(deviceId) == 0 {
		app.clientError(w, http.StatusBadRequest)
		return
	}

	// Body params
	var input RefreshInput
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
		&input.RefreshToken,
		deviceId,
	) {
		app.clientError(w, http.StatusUnauthorized)
		return
	}

	// TODO: consider changing current auth token and refresh token
	// Create new access token / refresh token pair
	token, err := app.GenerateUserAuthToken(
		r.Context(),
		userAuthToken.UserMongoId,
		userAuthToken.NetworkType,
		deviceId,
	)
	if err != nil {
		app.serverError(w, err)
		return
	}

	// Build response
	response := RefreshResponse{
		AccessToken:  token.AccessToken.Value,
		RefreshToken: token.RefreshToken,
	}

	marshaledResponse, err := json.Marshal(response)
	if err != nil {
		app.serverError(w, err)
		return
	}

	if _, err = w.Write(marshaledResponse); err != nil {
		app.serverError(w, err)
		return
	}
}
