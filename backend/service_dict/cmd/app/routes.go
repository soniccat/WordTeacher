package main

import (
	"net/http"

	"github.com/gorilla/mux"

	"service_dict/internal/routing/examples_v2"
	"service_dict/internal/routing/word"
	"service_dict/internal/routing/word_v2"
)

func (app *application) routes() *mux.Router {
	wordHandler := word.NewHandler(
		app.logger,
		app.timeProvider,
		app.sessionValidator,
		&app.wiktionaryRepositoryV1,
	)
	wordHandlerV2 := word_v2.NewHandler(
		app.logger,
		app.timeProvider,
		app.sessionValidator,
		&app.wiktionaryRepositoryV2,
	)
	examplesHandlerV2 := examples_v2.NewHandler(
		app.logger,
		app.timeProvider,
		app.sessionValidator,
		&app.wiktionaryRepositoryV2,
	)

	// Register handler functions.
	r := mux.NewRouter()
	r.Handle(
		"/api/dict/words/{term}",
		app.sessionManager.LoadAndSave(http.HandlerFunc(wordHandler.Word)),
	).Methods("GET")
	r.Handle(
		"/api/v2/dict/words/{term}",
		app.sessionManager.LoadAndSave(http.HandlerFunc(wordHandlerV2.Word)),
	).Methods("GET")
	r.Handle(
		"/api/v2/dict/examples/{text}",
		app.sessionManager.LoadAndSave(http.HandlerFunc(examplesHandlerV2.Examples)),
	).Methods("GET")

	return r
}
