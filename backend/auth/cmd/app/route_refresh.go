package main

import (
	"encoding/json"
	"errors"
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
		apphelpers.SetError(w, err, http.StatusBadRequest, app.logger)
		return
	}
	if len(session.Value) == 0 {
		apphelpers.SetError(w, errors.New("Session is empty"), http.StatusBadRequest, app.logger)
		return
	}

	// Header params
	var deviceId = r.Header.Get(apphelpers.HeaderDeviceId)
	if len(deviceId) == 0 {
		apphelpers.SetError(w, errors.New("DeviceId is session"), http.StatusBadRequest, app.logger)
		return
	}

	var deviceType = r.Header.Get(apphelpers.HeaderDeviceType)
	if len(deviceType) == 0 {
		apphelpers.SetError(w, errors.New("DeviceType is session"), http.StatusBadRequest, app.logger)
		return
	}

	// Body params
	var input RefreshInput
	err = json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		apphelpers.SetError(w, err, http.StatusBadRequest, app.logger)
		return
	}

	userAuthToken, err := userauthtoken.Load(r.Context(), app.sessionManager)
	if err != nil {
		apphelpers.SetError(w, err, http.StatusInternalServerError, app.logger)
		return
	}

	if !userAuthToken.IsValid() {
		apphelpers.SetError(w, errors.New("Token is invalid"), http.StatusUnauthorized, app.logger)
		return
	}

	if !userAuthToken.IsMatched(
		input.AccessToken,
		&input.RefreshToken,
		deviceType,
		deviceId,
	) {
		apphelpers.SetError(w, errors.New("Token is invalid"), http.StatusUnauthorized, app.logger)
		return
	}

	// TODO: consider changing current auth token and refresh token
	// Create new access token / refresh token pair
	token, err := app.GenerateUserAuthToken(
		r.Context(),
		userAuthToken.UserMongoId,
		userAuthToken.NetworkType,
		deviceType,
		deviceId,
	)
	if err != nil {
		apphelpers.SetError(w, err, http.StatusInternalServerError, app.logger)
		return
	}

	// Build response
	response := RefreshResponse{
		AccessToken:  token.AccessToken.Value,
		RefreshToken: token.RefreshToken,
	}

	apphelpers.WriteResponse(w, response, app.logger)
}
