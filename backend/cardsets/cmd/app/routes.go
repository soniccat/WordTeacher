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
	//	app.sessionManager.LoadAndSave(http.HandlerFunc(app.cardSetPush)),
	//).Methods("POST")
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