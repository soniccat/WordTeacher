package main

import (
	"models/apphelpers"
	"models/user"
	"net/http"
)

func (app *application) cardSetPull(w http.ResponseWriter, r *http.Request) {
	input, authToken, err := user.ValidateSession[CardSetSyncInput](r, app.sessionManager)
	if err != nil {
		apphelpers.SetError(w, err.InnerError, err.StatusCode, app.logger)
		return
	}
}
