package main

import (
	"auth/cmd/sessiondata"
	"encoding/json"
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

//	Purpose:
//
//	In:
//		Header: deviceId
//		Body: RefreshInput
//	Out:
//		RefreshResponse
//
func (app *application) refresh(w http.ResponseWriter, r *http.Request) {
	session, err := r.Cookie(CookieSession)
	if err != nil {
		app.clientError(w, http.StatusBadRequest)
		return
	}
	if len(session.Value) == 0 {
		app.clientError(w, http.StatusBadRequest)
		return
	}

	// Header params
	var deviceId = r.Header.Get(HeaderDeviceId)
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

	sessionData, err := sessiondata.Load(r.Context(), app.sessionManager)
	if err != nil {
		app.serverError(w, err)
		return
	}

	if !sessionData.IsValid() {
		app.clientError(w, http.StatusUnauthorized)
		return
	}

	if !sessionData.IsMatched(
		input.AccessToken,
		input.RefreshToken,
		deviceId,
	) {
		app.clientError(w, http.StatusBadRequest)
		return
	}

	// TODO: consider changing current auth token token changing
	// Create new access token / refresh token pair
	token, err := app.userModel.InsertUserAuthToken(
		r.Context(),
		&sessionData.UserMongoId,
		sessionData.NetworkType,
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
