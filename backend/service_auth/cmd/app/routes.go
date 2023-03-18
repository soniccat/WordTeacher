package main

import (
	"github.com/gorilla/mux"
	"net/http"
)

func (app *application) routes() *mux.Router {
	// Register handler functions.
	r := mux.NewRouter()
	r.Handle(
		"/api/service_auth/social/{networkType}", // TODO: adds versioning
		app.sessionManager.LoadAndSave(http.HandlerFunc(app.auth)),
	).Methods("POST")

	r.Handle(
		"/api/service_auth/refresh",
		app.sessionManager.LoadAndSave(http.HandlerFunc(app.refresh)),
	).Methods("POST")

	return r
}
