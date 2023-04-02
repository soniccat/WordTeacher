package main

import (
	"api"
	"errors"
	"net/http"
)

const (
	ArgumentQuery = "query"
)

type CardSetSearchResponse struct {
	CardSets []*api.CardSet `json:"cardSets,omitempty"`
}

func (app *application) cardSetSearch(w http.ResponseWriter, r *http.Request) {
	_, validateSessionErr := app.sessionValidator.Validate(r)
	if validateSessionErr != nil {
		app.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode)
		return
	}

	if r.Body == nil {
		app.SetError(w, errors.New("body is empty"), http.StatusBadRequest)
		return
	}

	var query = r.URL.Query().Get(ArgumentQuery)
	if len(query) == 0 {
		app.SetError(w, errors.New("query is empty"), http.StatusBadRequest)
		return
	}

	cardSets, err := app.cardSetSearchRepository.SearchCardSets(r.Context(), query)
	if err != nil {
		app.SetError(w, errors.New("query is empty"), http.StatusInternalServerError)
		return
	}

	response := CardSetSearchResponse{
		CardSets: cardSets,
	}
	app.WriteResponse(w, response)
}
