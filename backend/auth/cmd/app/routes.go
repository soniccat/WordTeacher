package main

import (
	"github.com/gorilla/mux"
	"net/http"
)

func (app *application) routes() *mux.Router {
	// Register handler functions.
	r := mux.NewRouter()
	r.Handle(
		"/api/auth/social/{networkType}",
		app.sessionManager.LoadAndSave(http.HandlerFunc(app.auth)),
	).Methods("POST")
	r.Handle(
		"/api/auth/refresh",
		app.sessionManager.LoadAndSave(http.HandlerFunc(app.refresh)),
	).Methods("POST")
	//r.HandleFunc("/api/movies/{id}", app.findByID).Methods("GET")
	//r.HandleFunc("/api/movies/", app.insert).Methods("POST")
	//r.HandleFunc("/api/movies/{id}", app.delete).Methods("DELETE")

	return r
}
