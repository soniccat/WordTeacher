package main

import (
	"context"
	"encoding/json"
	"errors"
	"models"
	"net/http"
	"tools"

	"github.com/gorilla/mux"
	"google.golang.org/api/idtoken"
)

// TODO: move in params
const GoogleIdTokenAndroidAudience = "435809636010-8kf32mn6jdokebe03cd9g8p2giudiq1c.apps.googleusercontent.com"
const GoogleIdDesktopTokenAudience = "166526384655-9ji25ddl02vg3d91g8vc2tbvbupl6o3k.apps.googleusercontent.com"

type AuthInput struct {
	Token string `json:"token,omitempty"`
}

type AuthResponse struct {
	Token AuthResponseToken `json:"token"`
	User  AuthResponseUser  `json:"user"`
}

type AuthResponseToken struct {
	AccessToken  string `json:"accessToken,omitempty"`
	RefreshToken string `json:"refreshToken,omitempty"`
}

type AuthResponseUser struct {
	Id          string `json:"id"`
	NetworkType string `json:"networkType"`
}

type AuthErrorInvalidToken struct {
	s string
}

func NewAuthErrorInvalidToken(str string) *AuthErrorInvalidToken {
	return &AuthErrorInvalidToken{
		s: str,
	}
}

func (e *AuthErrorInvalidToken) Error() string { return e.s }

// Purpose:
//
//	Validate input credentials and if everything is fine, generate new access token and refresh token
//
// In:
//
//	Path: 	networkType
//	Header: deviceId
//	Body: 	AuthInput
//
// Out:
//
//	AuthResponse
func (app *application) auth(w http.ResponseWriter, r *http.Request) {
	// Path params
	params := mux.Vars(r)
	networkType := params["networkType"]

	// Header params
	var deviceId = r.Header.Get(tools.HeaderDeviceId)
	if len(deviceId) == 0 {
		tools.SetError(w, errors.New("deviceId is empty"), http.StatusBadRequest, app.logger)
		return
	}

	var deviceType = r.Header.Get(tools.HeaderDeviceType)
	if len(deviceType) == 0 {
		tools.SetError(w, errors.New("deviceType is empty"), http.StatusBadRequest, app.logger)
		return
	}

	// Body params
	var credentials AuthInput
	err := json.NewDecoder(r.Body).Decode(&credentials)
	if err != nil {
		tools.SetError(w, err, http.StatusBadRequest, app.logger)
		return
	}

	// Resolve user and userNetwork
	var aUser *models.User
	var userNetwork *models.UserNetwork

	if networkType == "google" {
		aUser, userNetwork, err = app.resolveGoogleUser(r.Context(), &credentials, deviceType)

		if _, ok := err.(*AuthErrorInvalidToken); ok {
			tools.SetError(w, err, http.StatusUnauthorized, app.logger)
			return

		} else if err != nil {
			app.serverError(w, err)
			return
		}

	} else {
		app.serverError(w, errors.New("unknown networkType"))
		return
	}

	// Create user if needed
	if aUser == nil {
		aUser = &models.User{
			Networks: []models.UserNetwork{*userNetwork},
		}
		newUser, err := app.userModel.InsertUser(r.Context(), aUser)
		if err != nil {
			app.serverError(w, err)
			return
		}

		aUser = newUser
	} else {
		// TODO: add new network if needed
	}

	// Create new access token / refresh token pair
	token, err := app.GenerateUserAuthToken(
		r.Context(),
		&aUser.Id,
		userNetwork.NetworkType,
		deviceType,
		deviceId,
	)
	if err != nil {
		app.serverError(w, err)
		return
	}

	// Build response
	response := AuthResponse{
		Token: AuthResponseToken{
			AccessToken:  token.AccessToken.Value,
			RefreshToken: token.RefreshToken,
		},
		User: AuthResponseUser{
			Id:          aUser.Id.Hex(),
			NetworkType: networkType,
		},
	}

	tools.WriteResponse(w, response, app.logger)
}

func (app *application) resolveGoogleUser(
	context context.Context,
	credentials *AuthInput,
	deviceType string,
) (*models.User, *models.UserNetwork, error) {

	validator, err := idtoken.NewValidator(context)
	if err != nil {
		return nil, nil, err
	}

	var idToken string
	if deviceType == tools.DeviceTypeAndroid {
		idToken = GoogleIdTokenAndroidAudience
	} else {
		idToken = GoogleIdDesktopTokenAudience
	}

	// TODO: consider to make validation more strict
	payload, err := validator.Validate(context, credentials.Token, idToken)
	if err != nil {
		return nil, nil, NewAuthErrorInvalidToken(err.Error())
	}

	googleUserId, ok := payload.Claims["sub"].(string)
	if !ok {
		return nil, nil, NewAuthErrorInvalidToken("google Id Token doesn't have a sub")
	}

	googleEmail, ok := payload.Claims["email"].(string)
	if !ok {
		return nil, nil, NewAuthErrorInvalidToken("google Id Token doesn't have an email")
	}

	googleName, ok := payload.Claims["name"].(string)
	if !ok {
		return nil, nil, NewAuthErrorInvalidToken("google Id Token doesn't have a name")
	}

	googleUser, err := app.userModel.FindGoogleUser(context, &googleUserId)
	if err != nil {
		return nil, nil, err
	}

	// TODO: if user exists, update its network if it changed
	userNetwork := &models.UserNetwork{
		NetworkType:   models.Google,
		NetworkUserId: googleUserId,
		Email:         googleEmail,
		Name:          googleName,
	}

	return googleUser, userNetwork, nil
}
