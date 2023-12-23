package main

import (
	"net/http"

	"github.com/gorilla/mux"

	"service_cardsets/internal/routing/cardset_by_id"
	"service_cardsets/internal/routing/cardset_pull"
	"service_cardsets/internal/routing/cardset_push"
)

func (app *application) routes() *mux.Router {
	cardSetPushHandler := cardset_push.NewHandler(
		app.logger,
		app.timeProvider,
		app.sessionValidator,
		app.cardSetRepository,
	)
	cardSetPullHandler := cardset_pull.NewHandler(
		app.logger,
		app.timeProvider,
		app.sessionValidator,
		app.cardSetRepository,
	)
	cardSetByIdHandler := cardset_by_id.NewHandler(
		app.logger,
		app.timeProvider,
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
