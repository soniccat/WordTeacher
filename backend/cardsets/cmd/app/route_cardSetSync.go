package main

import (
	"encoding/json"
	"errors"
	"go.mongodb.org/mongo-driver/bson/primitive"
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
	CardSetCreate CardSetOperationType = iota // input: cardSet objects, operation guid for deduplication; resultOk: created CardSet Id
	CardSetDelete                             // input: cardSet ids; resultOk: nothing
	CardSetUpdate                             // input: cardSet objects; resultOk: nothing
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

type CardSetUpdateOperation struct {
	CardSet cardset.CardSetApi
}

type CardSetDeleteOperation struct {
	Ids []primitive.ObjectID
}

func (input *CardSetSyncInput) GetAccessToken() string {
	return input.AccessToken
}

func (input *CardSetSyncInput) GetRefreshToken() *string {
	return nil
}

type CardSetSyncResponse struct {
	Results         []CardSetOperationResult `json:"results"`
	UpdatedCardSets []cardset.CardSetApi     `json:"updatedCardSets,omitempty"`
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

	operations, parseErr := input.parseOperations()
	if parseErr != nil {
		apphelpers.SetError(w, parseErr, http.StatusBadRequest, app.logger)
		return
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
		case CardSetUpdateOperation:

		default:

		}
	}

	response.Results = results

	// Build response

	marshaledResponse, err2 := json.Marshal(response)
	if err != nil {
		apphelpers.SetError(w, err, http.StatusInternalServerError, app.logger)
		return
	}

	if _, err2 = w.Write(marshaledResponse); err2 != nil {
		apphelpers.SetError(w, err, http.StatusInternalServerError, app.logger)
		return
	}
}

func (input *CardSetSyncInput) parseOperations() ([]interface{}, error) {
	var operations []interface{}

	for _, rawOperation := range input.Operations {
		switch tp := rawOperation.Type; tp {
		case CardSetCreate:
			var cardSet cardset.CardSetApi
			err := json.Unmarshal(rawOperation.Arguments, &cardSet)

			if err != nil {
				return nil, err
			} else if cardSet.CreationId == nil {
				return nil, errors.New("CardSetCreate: CardSetApi CreationId is nil")
			} else {
				operations = append(operations, CardSetCreateOperation{CardSet: cardSet})
			}

		case CardSetDelete:
			var cardSetRawIds []string
			err := json.Unmarshal(rawOperation.Arguments, &cardSetRawIds)

			if err != nil {
				return nil, err
			} else {
				var cardSetIds []primitive.ObjectID
				for _, stringId := range cardSetRawIds {
					id, err := primitive.ObjectIDFromHex(stringId)
					if err != nil {
						return nil, errors.New("CardSetDelete: wrong cardSet id")
					} else {
						cardSetIds = append(cardSetIds, id)
					}
				}

				operations = append(operations, CardSetDeleteOperation{Ids: cardSetIds})
			}

		case CardSetUpdate:
			var cardSet cardset.CardSetApi
			err := json.Unmarshal(rawOperation.Arguments, &cardSet)

			if err != nil {
				return nil, err
			} else if len(cardSet.Id) == 0 {
				return nil, errors.New("CardSetUpdate: CardSetApi id is empty")
			} else {
				operations = append(operations, CardSetUpdateOperation{CardSet: cardSet})
			}
		}
	}
	return operations, nil
}
