package main

import (
	"encoding/json"
	"errors"
	"models"
	"net/http"
	"tools"
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
	session, err := r.Cookie(tools.CookieSession)
	if err != nil {
		tools.SetError(w, err, http.StatusUnauthorized, app.logger)
		return
	}
	if len(session.Value) == 0 {
		tools.SetError(w, errors.New("session is empty"), http.StatusBadRequest, app.logger)
		return
	}

	// Header params
	var deviceId = r.Header.Get(tools.HeaderDeviceId)
	if len(deviceId) == 0 {
		tools.SetError(w, errors.New("DeviceId is empty"), http.StatusBadRequest, app.logger)
		return
	}

	var deviceType = r.Header.Get(tools.HeaderDeviceType)
	if len(deviceType) == 0 {
		tools.SetError(w, errors.New("DeviceType is empty"), http.StatusBadRequest, app.logger)
		return
	}

	// Body params
	var input RefreshInput
	err = json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		tools.SetError(w, err, http.StatusBadRequest, app.logger)
		return
	}

	userAuthToken, err := models.Load(r.Context(), app.sessionManager)
	if err != nil {
		tools.SetError(w, err, http.StatusInternalServerError, app.logger)
		return
	}

	if !userAuthToken.IsValid() {
		tools.SetError(w, errors.New("token is invalid"), http.StatusUnauthorized, app.logger)
		return
	}

	if !userAuthToken.IsMatched(
		input.AccessToken,
		&input.RefreshToken,
		deviceType,
		deviceId,
	) {
		tools.SetError(w, errors.New("token is invalid"), http.StatusUnauthorized, app.logger)
		return
	}

	token, err := app.GenerateUserAuthToken(
		r.Context(),
		userAuthToken.UserMongoId,
		userAuthToken.NetworkType,
		deviceType,
		deviceId,
	)
	if err != nil {
		tools.SetError(w, err, http.StatusInternalServerError, app.logger)
		return
	}

	// Build response
	response := RefreshResponse{
		AccessToken:  token.AccessToken.Value,
		RefreshToken: token.RefreshToken,
	}

	tools.WriteResponse(w, response, app.logger)
}
