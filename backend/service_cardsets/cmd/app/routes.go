package main

import (
	"net/http"

	"github.com/gorilla/mux"

	"service_cardsets/internal/routing"
)

func (app *application) routes() *mux.Router {
	cardSetPushHandler := routing.NewCardSetPushHandler(
		app.logger,
		app.sessionValidator,
		app.cardSetRepository,
	)
	cardSetPullHandler := routing.NewCardSetPullHandler(
		app.logger,
		app.sessionValidator,
		app.cardSetRepository,
	)
	cardSetByIdHandler := routing.NewCardSetByIdHandler(
		app.logger,
		app.sessionValidator,
		app.cardSetRepository,
	)

	// Register handler functions.
	r := mux.NewRouter()
	r.Handle(
		"/api/cardsets/push",
		app.sessionManager.LoadAndSave(http.HandlerFunc(cardSetPushHandler.CardSetPush)),
	).Methods("POST")
	r.Handle(
		"/api/cardsets/pull",
		app.sessionManager.LoadAndSave(http.HandlerFunc(cardSetPullHandler.CardSetPull)),
	).Methods("POST")
	r.Handle(
		"/api/cardsets/{id}",
		app.sessionManager.LoadAndSave(http.HandlerFunc(cardSetByIdHandler.CardSetById)),
	).Methods("GET")

	return r
}
