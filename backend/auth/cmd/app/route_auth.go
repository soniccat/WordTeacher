package main

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/gorilla/mux"
	"google.golang.org/api/idtoken"
	"models/apphelpers"
	"models/user"
	"models/usernetwork"
	"net/http"
)

const GoogleIdTokenAudience = "409354406675-eqcftgj7fi5m4ri5s78r33kguqj2mgo3.apps.googleusercontent.com"

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
	Id string `json:"id"`
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
	var deviceId = r.Header.Get(apphelpers.HeaderDeviceId)
	if len(deviceId) == 0 {
		apphelpers.SetError(w, errors.New("deviceId is empty"), http.StatusBadRequest, app.logger)
		return
	}

	// Body params
	var credentials AuthInput
	err := json.NewDecoder(r.Body).Decode(&credentials)
	if err != nil {
		apphelpers.SetError(w, err, http.StatusBadRequest, app.logger)
		return
	}

	// Resolve user and userNetwork
	var aUser *user.User
	var userNetwork *usernetwork.UserNetwork

	if networkType == "google" {
		aUser, userNetwork, err = app.resolveGoogleUser(r.Context(), &credentials)

		if _, ok := err.(*AuthErrorInvalidToken); ok {
			apphelpers.SetError(w, err, http.StatusBadRequest, app.logger)
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
		aUser = &user.User{
			Networks: []usernetwork.UserNetwork{*userNetwork},
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
		&aUser.ID,
		userNetwork.NetworkType,
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
			Id: aUser.ID.Hex(),
		},
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

func (app *application) resolveGoogleUser(
	context context.Context,
	credentials *AuthInput,
) (*user.User, *usernetwork.UserNetwork, error) {

	validator, err := idtoken.NewValidator(context)
	if err != nil {
		return nil, nil, err
	}

	// TODO: consider to make validation more strict
	payload, err := validator.Validate(context, credentials.Token, GoogleIdTokenAudience)
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
	userNetwork := &usernetwork.UserNetwork{
		NetworkType:   usernetwork.Google,
		NetworkUserId: googleUserId,
		Email:         googleEmail,
		Name:          googleName,
	}

	return googleUser, userNetwork, nil
}
