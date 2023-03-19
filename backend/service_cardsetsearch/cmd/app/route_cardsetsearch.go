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
	UpdatedCardSets   []*api.CardSet `json:"updatedCardSets,omitempty"`
	DeletedCardSetIds []string       `json:"deletedCardSetIds,omitempty"`
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

	//ctx := r.Context()
	//
	//apiCardSets, err := app.cardSetSearchRepository.ModifiedCardSetsSince(ctx, authToken.UserMongoId, lastModificationDate)
	//if err != nil {
	//	app.SetError(w, err, http.StatusInternalServerError)
	//	return
	//}
}
