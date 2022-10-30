package main

import (
	"github.com/gorilla/mux"
	"net/http"
)

func (app *application) routes() *mux.Router {
	// Register handler functions.
	r := mux.NewRouter()
	r.Handle(
		"/api/cardsets/upload",
		app.sessionManager.LoadAndSave(http.HandlerFunc(app.cardSetUpload)),
	).Methods("POST")

	return r
}
