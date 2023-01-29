package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"models/apphelpers"
	"models/cardset"
	"models/tools"
	"net/http"
	"sync"
	"time"

	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/writeconcern"
)

const (
	ParameterLatestCardSetModificationDate = "latestCardSetModificationDate"
)

type CardSetPushInput struct {
	// for card sets without id creates a card set or find already inserted one with deduplication Id.
	// for card sets with id write a card set data
	UpdatedCardSets   []*cardset.ApiCardSet `json:"updatedCardSets"`
	CurrentCardSetIds []string              `json:"currentCardSetIds"`
}

type CardSetPushResponse struct {
	CardSetIds map[string]string `json:"cardSetIds,omitempty"` // deduplication id to primitive.ObjectID
	CardIds    map[string]string `json:"cardIds,omitempty"`    // deduplication id to primitive.ObjectID
}

func NewCardSetSyncResponse() *CardSetPushResponse {
	return &CardSetPushResponse{
		make(map[string]string),
		make(map[string]string),
	}
}

type UserMutex struct {
	mutexes map[primitive.ObjectID]*sync.Mutex
}

func NewUserMutex() *UserMutex {
	return &UserMutex{
		make(map[primitive.ObjectID]*sync.Mutex),
	}
}

func (um *UserMutex) lockForUser(u *primitive.ObjectID) {
	m, ok := um.mutexes[*u]
	if !ok {
		m = &sync.Mutex{}
		um.mutexes[*u] = m
	}

	m.Lock()
}

func (um *UserMutex) unlockForUser(u *primitive.ObjectID) {
	if m, ok := um.mutexes[*u]; ok {
		m.Unlock()
	}
}

var userMutex = NewUserMutex()

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
	authToken, validateSessionErr := app.sessionValidator.Validate(r)
	if validateSessionErr != nil {
		app.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode)
		return
	}

	var input CardSetPushInput
	err := json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		app.SetError(w, err, http.StatusBadRequest)
		return
	}

	query := r.URL.Query()
	if !query.Has(ParameterLatestCardSetModificationDate) {
		app.SetError(w, fmt.Errorf("%s is missing", ParameterLatestCardSetModificationDate), http.StatusBadRequest)
		return
	}

	lastModificationDate, err := time.Parse(time.RFC3339, r.URL.Query().Get(ParameterLatestCardSetModificationDate))
	if err != nil {
		app.SetError(w, err, http.StatusBadRequest)
	}

	hasModifications, err := app.cardSetRepository.HasModificationsSince(r.Context(), authToken.UserMongoId, lastModificationDate)
	if err != nil {
		app.SetError(w, err, http.StatusInternalServerError)
		return
	}
	if hasModifications {
		app.SetError(w, errors.New("data has been modified, pull is required"), http.StatusConflict)
		return
	}

	// validate DeletedCardSets
	currentCardSetIds, err := tools.IdsToMongoIds(input.CurrentCardSetIds)
	if err != nil {
		app.SetError(w, err, http.StatusBadRequest)
	}

	// execute all the changes in one transaction
	userMutex.lockForUser(authToken.UserMongoId)
	defer func() { userMutex.unlockForUser(authToken.UserMongoId) }()

	ctx := r.Context()
	session, sessionErr := app.cardSetRepository.MongoClient.StartSession()
	if sessionErr != nil {
		app.SetError(w, sessionErr, http.StatusInternalServerError)
		return
	}

	defer func() {
		session.EndSession(ctx)
	}()

	wc := writeconcern.New(writeconcern.WMajority())
	txnOpts := options.Transaction().SetWriteConcern(wc)
	response, err := session.WithTransaction(
		ctx,
		func(sCtx mongo.SessionContext) (interface{}, error) {
			response, sErr := app.handleUpdatedCardSets(sCtx, input.UpdatedCardSets, authToken.UserMongoId)
			if sErr != nil {
				return nil, sErr
			}

			insertedMongoIds, sErr := tools.IdsToMongoIds(tools.MapValues(response.CardSetIds))
			if sErr != nil {
				return nil, sErr
			}

			currentCardSetIdsWithAdded := append(currentCardSetIds, insertedMongoIds...)
			sErr = app.cardSetRepository.DeleteNotInList(sCtx, currentCardSetIdsWithAdded)
			if sErr != nil {
				return nil, sErr
			}

			return response, nil
		},
		txnOpts,
	)

	if err != nil {
		if updatedCardSetsError, ok := err.(*apphelpers.HandlerError); ok {
			app.SetHandlerError(w, updatedCardSetsError)
		} else {
			app.SetError(w, err, http.StatusInternalServerError)
		}
		return
	}

	app.WriteResponse(w, response)
}

func (app *application) handleUpdatedCardSets(
	ctx context.Context, // transaction is required
	updatedCardSets []*cardset.ApiCardSet,
	userId *primitive.ObjectID,
) (*CardSetPushResponse, error) {
	// validate
	cardWithoutIds := make(map[string]bool)

	for _, cardSet := range updatedCardSets {
		if len(cardSet.Id) == 0 && len(cardSet.CreationId) == 0 {
			return nil, app.NewHandlerError(http.StatusBadRequest, errors.New("card set without id has no creation id too"))
		}

		for _, card := range cardSet.Cards {
			if len(card.Id) == 0 {
				if len(card.CreationId) == 0 {
					return nil, app.NewHandlerError(http.StatusBadRequest, errors.New("card without id has no creation id too"))
				}

				cardWithoutIds[card.CreationId] = true
			}
		}
	}

	response := NewCardSetSyncResponse()

	for _, cardSet := range updatedCardSets {
		if len(cardSet.Id) == 0 {
			existingCardSet, err := app.cardSetRepository.FindCardSetByCreationId(ctx, cardSet.CreationId)
			if err != nil {
				return nil, app.NewHandlerError(http.StatusInternalServerError, err)
			}

			var cardSetId string
			if existingCardSet != nil {
				cardSetId = existingCardSet.Id.Hex()
				cardSet.Id = cardSetId
				cardSet.UserId = userId.Hex()
				errWithCode := app.cardSetRepository.UpdateCardSet(ctx, cardSet)
				if errWithCode != nil {
					return nil, app.NewHandlerError(errWithCode.Code, errWithCode.Err)
				}
			} else {
				_, errWithCode := app.cardSetRepository.InsertCardSet(ctx, cardSet, userId)
				if errWithCode != nil {
					return nil, app.NewHandlerError(errWithCode.Code, errWithCode.Err)
				}
				cardSetId = cardSet.Id
			}

			response.CardSetIds[cardSet.CreationId] = cardSetId

		} else {
			errWithCode := app.cardSetRepository.UpdateCardSet(ctx, cardSet)
			if errWithCode != nil {
				return nil, app.NewHandlerError(errWithCode.Code, errWithCode.Err)
			}
		}

		for _, card := range cardSet.Cards {
			if _, ok := cardWithoutIds[card.CreationId]; ok {
				response.CardIds[card.CreationId] = card.Id
			}
		}
	}

	return response, nil
}
