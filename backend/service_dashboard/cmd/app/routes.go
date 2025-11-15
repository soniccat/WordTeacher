package main

import (
	"net/http"

	"github.com/gorilla/mux"
	"github.com/prometheus/client_golang/prometheus/promhttp"

	"service_dashboard/internal/routing/dashboard"
)

func (app *application) routes() *mux.Router {
	dashboardHandler := dashboard.New(
		app.logger,
		app.timeProvider,
		app.sessionValidator,
		app.headlineStorage,
		app.cardsetStorage,
	)

	r := mux.NewRouter()
	r.Handle(
		"/api/v1/dashboard",
		app.sessionManager.LoadAndSave(http.HandlerFunc(dashboardHandler.Handle)),
	).Methods("GET")

	r.Handle(
		"/metrics",
		promhttp.Handler(),
	).Methods("GET")

	return r
}
