package main

import (
	"net/http"

	"service_articles/internal/routing/headlines"

	"github.com/gorilla/mux"
)

func (app *application) routes() *mux.Router {
	headlinesHandler := headlines.NewHandler(
		app.logger,
		app.timeProvider,
		app.sessionValidator,
		app.headlineRepository,
	)

	// Register handler functions.
	r := mux.NewRouter()
	r.Handle(
		"/api/v1/hadlines",
		app.sessionManager.LoadAndSave(http.HandlerFunc(headlinesHandler.Headlines)),
	).Methods("GET")

	return r
}
