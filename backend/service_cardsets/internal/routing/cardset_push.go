package routing

import (
	"api"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"sync"
	"time"
	"tools"
	"tools/logger"

	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/writeconcern"

	"models/session_validator"
	"service_cardsets/internal/storage"
)

const (
	ParameterLatestCardSetModificationDate = "latestCardSetModificationDate"
)

type CardSetPushInput struct {
	// for card sets without id creates a card set or find already inserted one with deduplication Id.
	// for card sets with id write a card set data
	UpdatedCardSets   []*api.CardSet `json:"updatedCardSets"`
	CurrentCardSetIds []string       `json:"currentCardSetIds"`
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

type CardSetPushHandler struct {
	tools.BaseHandler
	sessionValidator  session_validator.SessionValidator
	cardSetRepository *storage.Repository
}

func NewCardSetPushHandler(
	logger *logger.Logger,
	sessionValidator session_validator.SessionValidator,
	cardSetRepository *storage.Repository,
) *CardSetPushHandler {
	return &CardSetPushHandler{
		BaseHandler:       *tools.NewBaseHandler(logger),
		sessionValidator:  sessionValidator,
		cardSetRepository: cardSetRepository,
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
func (h *CardSetPushHandler) CardSetPush(w http.ResponseWriter, r *http.Request) {
	authToken, validateSessionErr := h.sessionValidator.Validate(r)
	if validateSessionErr != nil {
		h.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode)
		return
	}

	var input CardSetPushInput
	err := json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		h.SetError(w, err, http.StatusBadRequest)
		return
	}

	query := r.URL.Query()
	if !query.Has(ParameterLatestCardSetModificationDate) {
		h.SetError(w, fmt.Errorf("%s is missing", ParameterLatestCardSetModificationDate), http.StatusBadRequest)
		return
	}

	lastModificationDate, err := time.Parse(time.RFC3339, r.URL.Query().Get(ParameterLatestCardSetModificationDate))
	if err != nil {
		h.SetError(w, err, http.StatusBadRequest)
	}

	hasModifications, err := h.cardSetRepository.HasModificationsSince(r.Context(), authToken.UserMongoId, lastModificationDate)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}
	if hasModifications {
		h.SetError(w, errors.New("data has been modified, pull is required"), http.StatusConflict)
		return
	}

	// validate DeletedCardSets
	currentCardSetIds, err := tools.IdsToMongoIds(input.CurrentCardSetIds)
	if err != nil {
		h.SetError(w, err, http.StatusBadRequest)
	}

	// execute all the changes in one transaction
	userMutex.lockForUser(authToken.UserMongoId)
	defer func() { userMutex.unlockForUser(authToken.UserMongoId) }()

	ctx := r.Context()
	session, sessionErr := h.cardSetRepository.MongoClient.StartSession()
	if sessionErr != nil {
		h.SetError(w, sessionErr, http.StatusInternalServerError)
		return
	}

	defer func() {
		session.EndSession(ctx)
	}()

	var deletedIds []primitive.ObjectID
	wc := writeconcern.New(writeconcern.WMajority())
	txnOpts := options.Transaction().SetWriteConcern(wc)
	response, err := session.WithTransaction(
		ctx,
		func(sCtx mongo.SessionContext) (interface{}, error) {
			response, sErr := h.handleUpdatedCardSets(sCtx, input.UpdatedCardSets, authToken.UserMongoId)
			if sErr != nil {
				return nil, sErr
			}

			insertedMongoIds, sErr := tools.IdsToMongoIds(tools.MapValues(response.CardSetIds))
			if sErr != nil {
				return nil, sErr
			}

			currentCardSetIdsWithAdded := append(currentCardSetIds, insertedMongoIds...)
			deletedIds, sErr = h.cardSetRepository.IdsNotInList(sCtx, currentCardSetIdsWithAdded)
			if sErr != nil {
				return nil, sErr
			}

			sErr = h.cardSetRepository.MarkAsDeletedByIds(sCtx, deletedIds)
			if sErr != nil {
				return nil, sErr
			}

			return response, nil
		},
		txnOpts,
	)

	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}

	h.WriteResponse(w, response)
}

func (h *CardSetPushHandler) handleUpdatedCardSets(
	ctx context.Context, // transaction is required
	updatedCardSets []*api.CardSet,
	userId *primitive.ObjectID,
) (*CardSetPushResponse, error) {
	// validate
	cardWithoutIds := make(map[string]bool)

	for _, cardSet := range updatedCardSets {
		if len(cardSet.Id) == 0 && len(cardSet.CreationId) == 0 {
			return nil, h.NewHandlerError(http.StatusBadRequest, errors.New("card set without id has no creation id too"))
		}

		for _, card := range cardSet.Cards {
			if len(card.Id) == 0 {
				if len(card.CreationId) == 0 {
					return nil, h.NewHandlerError(http.StatusBadRequest, errors.New("card without id has no creation id too"))
				}

				cardWithoutIds[card.CreationId] = true
			}
		}
	}

	response := NewCardSetSyncResponse()

	for _, cardSet := range updatedCardSets {
		cardSet.UserId = userId.Hex()

		if len(cardSet.Id) == 0 {
			existingCardSet, err := h.cardSetRepository.FindCardSetByCreationId(ctx, cardSet.CreationId)
			if err != nil {
				return nil, h.NewHandlerError(http.StatusInternalServerError, err)
			}

			var cardSetId string
			if existingCardSet != nil {
				cardSetId = existingCardSet.Id.Hex()
				cardSet.Id = cardSetId
				errWithCode := h.cardSetRepository.UpdateCardSet(ctx, cardSet)
				if errWithCode != nil {
					return nil, h.NewHandlerError(errWithCode.Code, errWithCode.Err)
				}
			} else {
				_, errWithCode := h.cardSetRepository.InsertCardSet(ctx, cardSet, userId)
				if errWithCode != nil {
					return nil, h.NewHandlerError(errWithCode.Code, errWithCode.Err)
				}
				cardSetId = cardSet.Id
			}

			response.CardSetIds[cardSet.CreationId] = cardSetId

		} else {
			errWithCode := h.cardSetRepository.UpdateCardSet(ctx, cardSet)
			if errWithCode != nil {
				return nil, h.NewHandlerError(errWithCode.Code, errWithCode.Err)
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