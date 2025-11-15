package main

import (
	"net/http"

	"github.com/gorilla/mux"
	"github.com/prometheus/client_golang/prometheus/promhttp"

	"service_cardsetsearch/internal/routing"
)

func (app *application) routes() *mux.Router {
	cardSetSearchHandler := routing.NewCardSetSearchHandler(
		app.logger,
		app.timeProvider,
		app.sessionValidator,
		app.cardSetSearchRepository,
	)

	r := mux.NewRouter()
	r.Handle(
		"/api/cardsets/search",
		app.sessionManager.LoadAndSave(http.HandlerFunc(cardSetSearchHandler.CardSetSearch)),
	).Methods("GET")

	r.Handle(
		"/metrics",
		promhttp.Handler(),
	).Methods("GET")

	return r
}
