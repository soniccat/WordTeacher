package main

import (
	"net/http"

	"github.com/gorilla/mux"

	"service_cardsetsearch/internal/routing"
)

func (app *application) routes() *mux.Router {
	cardSetSearchHandler := routing.NewCardSetSearchHandler(app.sessionValidator, app.cardSetSearchRepository)

	r := mux.NewRouter()
	r.Handle(
		"/api/cardsets/search",
		app.sessionManager.LoadAndSave(http.HandlerFunc(cardSetSearchHandler.CardSetSearch)),
	).Methods("GET")

	return r
}
