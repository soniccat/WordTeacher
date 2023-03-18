package main

import (
	"fmt"
	"net/http"
	"runtime/debug"
)

// TODO: replace with
func (app *application) serverError(w http.ResponseWriter, err error) {
	trace := fmt.Sprintf("%s\n%s", err.Error(), debug.Stack())
	app.logger.Error.Output(2, trace)

	http.Error(w, http.StatusText(http.StatusInternalServerError), http.StatusInternalServerError)
}

func (app *application) clientError(w http.ResponseWriter, status int) {
	app.logger.Info.Printf("Client error: %s", debug.Stack())
	http.Error(w, http.StatusText(status), status)
}
