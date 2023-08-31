package main

import (
	"net/http"

	"github.com/gorilla/mux"
)

func (app *application) routes() *mux.Router {
	// Register handler functions.
	r := mux.NewRouter()
	r.Handle(
		"/1/web/adapt/",
		app.sessionManager.LoadAndSave(http.HandlerFunc(app.adapt)),
	).Methods("GET")

	return r
}
