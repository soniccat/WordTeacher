package main

import (
	"net/http"

	"github.com/gorilla/mux"

	"service_auth/internal/routing/auth"
	"service_auth/internal/routing/refresh"
)

func (app *application) routes() *mux.Router {
	authHandler := auth.New(
		app.logger,
		app.timeProvider,
		app.authorizer,
	)
	refreshHandler := refresh.New(
		app.logger,
		app.timeProvider,
		app.tokenRefresher,
	)

	r := mux.NewRouter()
	r.Handle(
		"/api/auth/social/{networkType}", // TODO: adds versioning
		app.sessionManager.LoadAndSave(http.HandlerFunc(authHandler.Auth)),
	).Methods("POST")
	r.Handle(
		"/api/auth/refresh",
		app.sessionManager.LoadAndSave(http.HandlerFunc(refreshHandler.Refresh)),
	).Methods("POST")

	return r
}
