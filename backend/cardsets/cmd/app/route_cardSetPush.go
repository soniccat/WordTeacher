package main

import (
	"context"
	"encoding/json"
	"errors"
	"models/apphelpers"
	"models/cardset"
	"models/tools"
	"models/user"
	"net/http"
	"sync"

	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/writeconcern"
)

const (
	ParameterLastPullDate = "lastPullDate"
	// ParameterPullUpdatedCardSets = "pullUpdatedCardSets"
)

type CardSetPushInput struct {
	AccessToken string `json:"accessToken"`
	// for card sets without id creates a card set or find already inserted one with deduplication Id.
	// for card sets with id write a card set data
	UpdatedCardSets []*cardset.CardSetApi `json:"updatedCardSets"`
	DeletedCardSets []string              `json:"deletedCardSets"`
}

func (input *CardSetPushInput) GetAccessToken() string {
	return input.AccessToken
}

func (input *CardSetPushInput) GetRefreshToken() *string {
	return nil
}

type CardSetSyncResponse struct {
	CardSetIds map[string]string `json:"cardSetIds,omitempty"` // deduplication id to primitive.ObjectID
	CardIds    map[string]string `json:"cardIds,omitempty"`    // deduplication id to primitive.ObjectID
}

func NewCardSetSyncResponse() *CardSetSyncResponse {
	return &CardSetSyncResponse{
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
	input, authToken, validateSessionErr := user.ValidateSession[CardSetPushInput](r, app.sessionManager)
	if validateSessionErr != nil {
		apphelpers.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode, app.logger)
		return
	}

	//lastPullDate, err := time.Parse(time.RFC3339, r.URL.Query().Get(ParameterLastPullDate))
	//if err != nil {
	//	apphelpers.SetError(w, err, http.StatusBadRequest, app.logger)
	//}

	//TODO: find the current modification date and compare with lastPullDate
	// send an error if pulling is required

	// validate DeletedCardSets
	deletedCardDbIds, err := tools.MapOrError(input.DeletedCardSets, func(cardSetIdString string) (*primitive.ObjectID, error) {
		cardSetDbId, err := primitive.ObjectIDFromHex(cardSetIdString)
		return &cardSetDbId, err
	})
	if err != nil {
		apphelpers.SetError(w, err, http.StatusBadRequest, app.logger)
	}
	// execute all the changes in one transaction

	userMutex.lockForUser(authToken.UserMongoId)
	defer func() { userMutex.unlockForUser(authToken.UserMongoId) }()

	ctx := r.Context()
	session, sessionErr := app.cardSetModel.MongoClient.StartSession()
	if sessionErr != nil {
		apphelpers.SetError(w, sessionErr, http.StatusInternalServerError, app.logger)
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

			sErr = app.handleDeletedCardSets(sCtx, deletedCardDbIds)
			if sErr != nil {
				return nil, sErr
			}

			return response, nil
		},
		txnOpts,
	)

	if err != nil {
		if updatedCardSetsError, ok := err.(*apphelpers.HandlerError); ok {
			apphelpers.SetHandlerError(w, updatedCardSetsError, app.logger)
		} else {
			apphelpers.SetError(w, sessionErr, http.StatusInternalServerError, app.logger)
		}

		return
	}

	// Build response

	marshaledResponse, err := json.Marshal(response)
	if err != nil {
		apphelpers.SetError(w, err, http.StatusInternalServerError, app.logger)
		return
	}

	if _, err = w.Write(marshaledResponse); err != nil {
		apphelpers.SetError(w, err, http.StatusInternalServerError, app.logger)
		return
	}
}

func (app *application) handleUpdatedCardSets(
	ctx context.Context, // transaction is required
	updatedCardSets []*cardset.CardSetApi,
	userId *primitive.ObjectID,
) (*CardSetSyncResponse, error) {

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
			existingCardSet, err := app.cardSetModel.FindCardSetByCreationId(ctx, cardSet.CreationId)
			if err != nil {
				return nil, app.NewHandlerError(http.StatusInternalServerError, err)
			}

			var cardSetId string
			if existingCardSet != nil {
				cardSetId = existingCardSet.ID.Hex()
				cardSet.Id = existingCardSet.ID.Hex()
				errWithCode := app.cardSetModel.UpdateCardSet(ctx, cardSet)
				if errWithCode != nil {
					return nil, app.NewHandlerError(errWithCode.Code, errWithCode.Err)
				}
			} else {
				_, errWithCode := app.cardSetModel.InsertCardSet(ctx, cardSet, userId)
				if errWithCode != nil {
					return nil, app.NewHandlerError(errWithCode.Code, errWithCode.Err)
				}
				cardSetId = cardSet.Id
			}

			response.CardSetIds[cardSet.CreationId] = cardSetId

			// TODO: optimize that with updating instead of deleting
			// delete previously created cards
			//err := app.cardSetModel.DeleteCardSetByCreationId(ctx, cardSet.CreationId)
			//if err != nil {
			//	return nil, app.NewHandlerError(http.StatusInternalServerError, err)
			//
			//} else {
			//	insertedCardSet, err := app.cardSetModel.InsertCardSet(ctx, cardSet, userId)
			//	if err != nil {
			//		return nil, app.NewHandlerError(http.StatusInternalServerError, err)
			//	} else {
			//		response.CardSetIds[cardSet.CreationId] = insertedCardSet.Id
			//
			//		for _, card := range insertedCardSet.Cards {
			//			if _, ok := cardCreationIdSet[card.CreationId]; ok {
			//				response.CardIds[card.CreationId] = card.Id
			//			}
			//		}
			//	}
			//}
		} else {
			errWithCode := app.cardSetModel.UpdateCardSet(ctx, cardSet)
			if errWithCode != nil {
				return nil, app.NewHandlerError(errWithCode.Code, errWithCode.Err)
			}

			//err := app.cardSetModel.UpdateCardSet(
			//	ctx,
			//	cardSet,
			//)
			//if err != nil {
			//	return nil, app.NewHandlerError(http.StatusInternalServerError, err)
			//}
			//
			//for _, card := range cardSet.Cards {
			//	if _, ok := cardCreationIdSet[card.CreationId]; ok {
			//		response.CardIds[card.CreationId] = card.Id
			//	}
			//}
		}

		for _, card := range cardSet.Cards {
			if _, ok := cardWithoutIds[card.CreationId]; ok {
				response.CardIds[card.CreationId] = card.Id
			}
		}
	}

	return response, nil
}

func (app *application) handleDeletedCardSets(
	ctx context.Context,
	cardSetIds []*primitive.ObjectID,
) error {
	for _, id := range cardSetIds {
		err := app.cardSetModel.DeleteCardSet(ctx, id)
		if err != nil {
			return err
		}
	}

	return nil
}
