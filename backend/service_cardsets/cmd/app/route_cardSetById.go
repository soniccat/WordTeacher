package main

import (
	"api"
	"github.com/gorilla/mux"
	"github.com/pkg/errors"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"net/http"
)

type CardSetGetByIdResponse struct {
	CardSet *api.CardSet `json:"cardSet"`
}

func (app *application) cardSetById(w http.ResponseWriter, r *http.Request) {
	_, validateSessionErr := app.sessionValidator.Validate(r)
	if validateSessionErr != nil {
		app.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode)
		return
	}

	// Path params
	params := mux.Vars(r)
	cardSetId, ok := params["id"]
	if !ok {
		app.SetError(w, errors.New("id parameter is missing"), http.StatusBadRequest)
		return
	}

	cardSetDbId, err := primitive.ObjectIDFromHex(cardSetId)
	if err != nil {
		app.SetError(w, err, http.StatusBadRequest)
		return
	}

	dbCardSet, err := app.cardSetRepository.LoadCardSetDbByObjectID(r.Context(), cardSetDbId)
	if err != nil {
		app.SetError(w, err, http.StatusServiceUnavailable)
		return
	}

	apiCardSet := dbCardSet.ToApi()
	response := CardSetGetByIdResponse{
		CardSet: apiCardSet,
	}
	app.WriteResponse(w, response)
}
