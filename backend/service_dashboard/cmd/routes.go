package main

import (
	"net/http"
	"service_dashboard/internal/routing/dashboard"

	"github.com/gorilla/mux"
)

func (app *application) routes() *mux.Router {
	cardSetSearchHandler := dashboard.NewHandler(
		app.logger,
		app.timeProvider,
		app.sessionValidator,
		app.headlineStorage,
	)

	r := mux.NewRouter()
	r.Handle(
		"/api/v1/dashboard/",
		app.sessionManager.LoadAndSave(http.HandlerFunc(cardSetSearchHandler.CardSetSearch)),
	).Methods("GET")

	return r
}
