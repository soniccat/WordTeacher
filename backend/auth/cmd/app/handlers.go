package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"github.com/gorilla/mux"
	"google.golang.org/api/idtoken"
	"net/http"
)

const GoogleIdTokenAudience = "409354406675-eqcftgj7fi5m4ri5s78r33kguqj2mgo3.apps.googleusercontent.com"

func (app *application) auth(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	networkType := params["networkType"]

	var credentials SocialCredentials

	err := json.NewDecoder(r.Body).Decode(&credentials)
	if err != nil {
		app.serverError(w, err)
		return
	}

	//if foo, ok := app.sessionManager.Get(r.Context(), "message").(string); ok {
	//	app.infoLog.Printf("message is %s", foo)
	//}

	var user *User
	var userNetwork UserNetwork

	if networkType == "google" {
		validator, err := idtoken.NewValidator(r.Context())
		if err != nil {
			app.serverError(w, err)
			return
		}

		// TODO: consider to make validation more strict
		payload, err := validator.Validate(r.Context(), credentials.Token, GoogleIdTokenAudience)
		if err != nil {
			app.serverError(w, err)
			return
		}

		googleUserId, ok := payload.Claims["sub"].(string)
		if !ok {
			app.serverError(w, errors.New("google Id Token doesn't have a sub"))
			return
		}

		googleEmail, ok := payload.Claims["email"].(string)
		if !ok {
			app.serverError(w, errors.New("google Id Token doesn't have an email"))
			return
		}

		googleUser, err := app.userModel.FindGoogleUser(r.Context(), &googleUserId)
		if err != nil {
			app.serverError(w, err)
			return
		}

		user = googleUser

		// TODO: if user exists, update it's network if it changed
		userNetwork = UserNetwork{
			NetworkType:   NetworkGoogle,
			NetworkUserId: googleUserId,
			Token:         credentials.Token,
			Email:         googleEmail,
		}
	} else {
		app.serverError(w, err)
		return
	}

	if user == nil {
		user = &User{
			Networks: []UserNetwork{userNetwork},
		}
		newUser, err := app.userModel.InsertUser(r.Context(), user)
		if err != nil {
			app.serverError(w, err)
			return
		}

		user = newUser
	} else {
		// TODO: add new network if needed
	}

	key := app.sessionManager.Get(r.Context(), SessionUserIdKey)
	fmt.Printf("%s", key)

	// TODO: generate UserToken
	app.sessionManager.Put(r.Context(), SessionAccessTokenKey, credentials.Token)
	app.sessionManager.Put(r.Context(), SessionUserIdKey, user.ID.Hex())

	marshaledUser, err := json.Marshal(user)
	if err != nil {
		app.serverError(w, err)
		return
	}

	if _, err = w.Write(marshaledUser); err != nil {
		app.serverError(w, err)
		return
	}

	//app.logger.info.Printf("auth: user - ", marshaledUser)
}
