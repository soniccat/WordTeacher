package main

import (
	"encoding/json"
	"errors"
	"models/apphelpers"
	"models/cardset"
	"models/user"
	"net/http"
)

const (
	//ParameterLastSyncDate        = "lastSyncDate"
	ParameterPullUpdatedCardSets = "pullUpdatedCardSets"
)

type CardSetOperationType int

const (
	CardSetCreate CardSetOperationType = iota // input: cardSet objects, operation guid for deduplication, result CardSetApi
	CardSetDelete                             // input: cardSet ids
	CardSetUpdate                             // input: cardSet objects
)

type CardSetOperationResultType int

const (
	CardSetOperationResultOk CardSetOperationResultType = iota
	//CardSetOperationResultConflict                            // object was already modified, output: two objects
	CardSetOperationResultError
)

type CardSetSyncInput struct {
	AccessToken string                `json:"accessToken"`
	Operations  []CardSetRawOperation `json:"operations"`
}

type CardSetRawOperation struct {
	Type      CardSetOperationType `json:"type"`
	Arguments json.RawMessage      `json:"arguments"`
}

type CardSetCreateOperation struct {
	CardSet cardset.CardSetApi
}

func (i CardSetSyncInput) GetAccessToken() string {
	return i.AccessToken
}

func (i CardSetSyncInput) GetRefreshToken() *string {
	return nil
}

type CardSetSyncResponse struct {
	Results         []CardSetOperationResult `json:"results"`
	UpdatedCardSets []cardset.CardSetApi     `json:"updatedCardSets"`
}

type CardSetOperationResult struct {
	Type      CardSetOperationResultType `json:"type"`
	Arguments interface{}                `json:"arguments"`
}

// Purpose:
//
// In:
//
//	Header: deviceId
//	Body: RefreshInput
//
// Out:
//
//	RefreshResponse
func (app *application) cardSetSync(w http.ResponseWriter, r *http.Request) {
	input, authToken, err := user.ValidateSession[CardSetSyncInput](r, app.sessionManager)
	if err != nil {
		apphelpers.SetError(w, err.InnerError, err.StatusCode, app.logger)
		return
	}

	// For now the current merge policy is last request always wins

	//var lastSyncDate *time.Time
	//if r.URL.Query().Has(ParameterLastSyncDate) {
	//	lastSyncDate, err := time.Parse(time.RFC3339, r.URL.Query().Get(ParameterLastSyncDate))
	//	if err != nil {
	//		apphelpers.SetError(w, err, http.StatusBadRequest, app.logger)
	//	}
	//}

	//pullUpdatedCardSets := false
	//if r.URL.Query().Has(ParameterPullUpdatedCardSets) {
	//	pullUpdatedCardSets, err := strconv.ParseBool(r.URL.Query().Get(ParameterPullUpdatedCardSets))
	//	if err != nil {
	//		apphelpers.SetError(w, err, http.StatusBadRequest, app.logger)
	//	}
	//}

	// parse input into operations
	var operations []interface{}

	for _, rawOperation := range input.Operations {
		if rawOperation.Type == CardSetCreate {
			var cardSet cardset.CardSetApi
			err := json.Unmarshal(rawOperation.Arguments, &cardSet)

			if err != nil {
				apphelpers.SetError(w, err, http.StatusBadRequest, app.logger)
				return
			} else if cardSet.CreationId == nil {
				apphelpers.SetError(w, errors.New("CardSetApi CreationId is nil"), http.StatusBadRequest, app.logger)
				return
			} else {
				operations = append(operations, CardSetCreateOperation{CardSet: cardSet})
			}

		}
	}

	response := CardSetSyncResponse{}
	var results []CardSetOperationResult

	// execute
	for _, operation := range operations {
		switch v := operation.(type) {
		case CardSetCreateOperation:
			// delete previously created cards
			err := app.cardSetModel.DeleteCardSetByCreationId(r.Context(), *v.CardSet.CreationId)

			if err != nil {
				results = append(results, CardSetOperationResult{Type: CardSetOperationResultError})

			} else {
				insertedCardSet, err := app.cardSetModel.InsertCardSet(r.Context(), &v.CardSet, authToken.UserMongoId)
				if err != nil {
					results = append(results, CardSetOperationResult{Type: CardSetOperationResultError})
				} else {
					results = append(results, CardSetOperationResult{Type: CardSetOperationResultOk, Arguments: insertedCardSet.Id})
				}
			}
		default:

		}
	}

	response.Results = results
}
