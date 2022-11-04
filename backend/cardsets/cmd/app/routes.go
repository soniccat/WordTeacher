package main

import (
	"github.com/gorilla/mux"
	"net/http"
)

func (app *application) routes() *mux.Router {
	// Register handler functions.
	r := mux.NewRouter()
	// TODO: to get the current status of remote cardsets, should support ifModifiedSince
	//r.Handle(
	//	"/api/cardsets/status",
	//	app.sessionManager.LoadAndSave(http.HandlerFunc(app.cardSetSync)),
	//).Methods("POST")
	r.Handle(
		"/api/cardsets/sync",
		app.sessionManager.LoadAndSave(http.HandlerFunc(app.cardSetSync)),
	).Methods("POST")

	return r
}
