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
// ParameterLastSyncDate        = "lastSyncDate"
// ParameterPullUpdatedCardSets = "pullUpdatedCardSets"
)

type CardSetPushInput struct {
	AccessToken string `json:"accessToken"`
	// for card sets without id creates a card set or find already inserted one with deduplication Id.
	// for card sets with id write a card set data
	UpdatedCardSets []*cardset.CardSetApi `json:"updatedCardSets"`
	DeletedCardSets []*primitive.ObjectID `json:"deletedCardSets"`
}

func (input *CardSetPushInput) GetAccessToken() string {
	return input.AccessToken
}

func (input *CardSetPushInput) GetRefreshToken() *string {
	return nil
}

type CardSetSyncResponse struct {
	Ids map[string]string `json:"ids"` // deduplication id to primitive.ObjectID
}

// Purpose:
//
//	write passed data in DB, always treat the data as the most recent
//	doesn't change the passed data
//
// In:
//
//	Header: deviceId
//	Body: RefreshInput
//
// Out:
//
//	RefreshResponse
func (app *application) cardSetPush(w http.ResponseWriter, r *http.Request) {
	input, authToken, err := user.ValidateSession[CardSetPushInput](r, app.sessionManager)
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
		//case CardSetCreateOperation:
		//	// delete previously created cards
		//	err := app.cardSetModel.DeleteCardSetByCreationId(r.Context(), *v.CardSet.CreationId)
		//
		//	if err != nil {
		//		results = append(results, CardSetOperationResult{Type: CardSetOperationResultError})
		//
		//	} else {
		//		insertedCardSet, err := app.cardSetModel.InsertCardSet(r.Context(), &v.CardSet, authToken.UserMongoId)
		//		if err != nil {
		//			results = append(results, CardSetOperationResult{Type: CardSetOperationResultError})
		//		} else {
		//			results = append(results, CardSetOperationResult{Type: CardSetOperationResultOk, Arguments: insertedCardSet.Id})
		//		}
		//	}
		case CardSetUpdateOperation:

		case CardSetDeleteOperation:

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
