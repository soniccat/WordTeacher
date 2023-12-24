package main

import (
	"net/http"

	"github.com/gorilla/mux"

	"service_auth/internal/routing"
)

func (app *application) routes() *mux.Router {
	authHandler := routing.NewAuthHandler(
		app.logger,
		app.timeProvider,
		app.userRepository,
		app.userAuthTokenGenerator,
	)
	refreshHandler := routing.NewRefreshHandler(
		app.logger,
		app.timeProvider,
		app.sessionManager,
		app.userRepository,
		app.userAuthTokenGenerator,
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
