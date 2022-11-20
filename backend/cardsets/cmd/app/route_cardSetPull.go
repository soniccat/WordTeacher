package main

import (
	"net/http"
)

func (app *application) cardSetPull(w http.ResponseWriter, r *http.Request) {
	//input, authToken, err := user.ValidateSession[CardSetSyncInput](r, app.sessionManager)
	//if err != nil {
	//	apphelpers.SetError(w, err.InnerError, err.StatusCode, app.logger)
	//	return
	//}

	//var lastSyncDate *time.Time
	//if r.URL.Query().Has(ParameterLastSyncDate) {
	//	lastSyncDate, err := time.Parse(time.RFC3339, r.URL.Query().Get(ParameterLastSyncDate))
	//	if err != nil {
	//		apphelpers.SetError(w, err, http.StatusBadRequest, app.logger)
	//	}
	//}
}
