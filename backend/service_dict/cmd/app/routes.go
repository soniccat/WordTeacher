package main

import (
	"net/http"

	"github.com/gorilla/mux"

	"service_dict/internal/routing"
)

func (app *application) routes() *mux.Router {
	wordHandler := routing.NewWordHandler(
		app.logger,
		app.timeProvider,
		app.wiktionaryRepository,
	)

	// Register handler functions.
	r := mux.NewRouter()
	r.Handle(
		"/api/dict/words/{term}",
		app.sessionManager.LoadAndSave(http.HandlerFunc(wordHandler.Word)),
	).Methods("GET")

	return r
}
