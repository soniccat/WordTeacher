package main

import (
	"net/http"

	"github.com/gorilla/mux"
	"github.com/prometheus/client_golang/prometheus/promhttp"

	"service_articles/internal/routing/headlines"
)

func (app *application) routes() *mux.Router {
	headlinesHandler := headlines.NewHandler(
		app.logger,
		app.timeProvider,
		app.sessionValidator,
		app.headlineStorage,
	)

	// Register handler functions.
	r := mux.NewRouter()
	r.Handle(
		"/api/v1/headlines",
		app.sessionManager.LoadAndSave(http.HandlerFunc(headlinesHandler.Headlines)),
	).Methods("GET")

	r.Handle(
		"/metrics",
		promhttp.Handler(),
	).Methods("GET")

	return r
}
