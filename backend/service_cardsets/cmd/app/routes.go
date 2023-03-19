package main

import (
	"github.com/gorilla/mux"
	"net/http"
)

func (app *application) routes() *mux.Router {
	// Register handler functions.
	r := mux.NewRouter()
	r.Handle(
		"/api/cardsets/push",
		app.sessionManager.LoadAndSave(http.HandlerFunc(app.cardSetPush)),
	).Methods("POST")
	r.Handle(
		"/api/cardsets/pull",
		app.sessionManager.LoadAndSave(http.HandlerFunc(app.cardSetPull)),
	).Methods("POST")

	return r
}
