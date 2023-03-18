package main

import (
	"github.com/gorilla/mux"
	"net/http"
)

func (app *application) routes() *mux.Router {
	// Register handler functions.
	r := mux.NewRouter()
	// TODO: to get the current status of remote service_cardsets, should support ifModifiedSince
	//r.Handle(
	//	"/api/service_cardsets/status",
	//	app.sessionManager.LoadAndSave(http.HandlerFunc(app.cardSetPush)),
	//).Methods("POST")
	r.Handle(
		"/api/service_cardsets/push",
		app.sessionManager.LoadAndSave(http.HandlerFunc(app.cardSetPush)),
	).Methods("POST")
	r.Handle(
		"/api/service_cardsets/pull",
		app.sessionManager.LoadAndSave(http.HandlerFunc(app.cardSetPull)),
	).Methods("POST")

	return r
}
