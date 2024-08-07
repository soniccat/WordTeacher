package cardset_push

import (
	"api"
	"context"
	"encoding/json"
	"errors"
	"models"
	"net/http"
	"service_cardsets/internal/model"
	"sync"
	"time"
	"tools"
	"tools/logger"

	"models/session_validator"

	"github.com/google/uuid"
)

type storage interface {
	HasModificationsSince(ctx context.Context, userId string, lastModificationDate *time.Time) (bool, error)
	IdsNotInList(ctx context.Context, userId string, ids []string) ([]string, error)
	MarkAsDeletedByIds(ctx context.Context, ids []string, modificationDate time.Time) error
	UpdateCardSet(ctx context.Context, cardSet *api.CardSet) error
	InsertCardSet(ctx context.Context, cardSet *api.CardSet, userId string) (*api.CardSet, error)
	FindCardSetByCreationId(context context.Context, creationId string) (*model.DbCardSet, error)
	StartTransaction(ctx context.Context, block func(tCtx context.Context) (interface{}, error)) (interface{}, error)
}

type Input struct {
	// for card sets without id creates a card set or find already inserted one with deduplication Id.
	// for card sets with id write a card set data
	UpdatedCardSets        []*api.CardSet `json:"updatedCardSets"`
	CurrentCardSetIds      []string       `json:"currentCardSetIds"`
	LatestModificationDate *string        `json:"latestModificationDate,omitempty"`
}

type Response struct {
	CardSetIds             map[string]string `json:"cardSetIds,omitempty"`   // deduplication id to primitive.ObjectID
	CardIds                map[string]string `json:"cardIds,omitempty"`      // deduplication id to primitive.ObjectID
	LatestModificationDate string            `json:"latestModificationDate"` // includes deleted cardSet modificationDate
}

func NewResponse() *Response {
	return &Response{
		make(map[string]string),
		make(map[string]string),
		"",
	}
}

type Handler struct {
	tools.BaseHandler
	sessionValidator session_validator.SessionValidator
	storage          storage
}

func NewHandler(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionValidator session_validator.SessionValidator,
	storage storage,
) *Handler {
	return &Handler{
		BaseHandler:      *tools.NewBaseHandler(logger, timeProvider),
		sessionValidator: sessionValidator,
		storage:          storage,
	}
}

// TODO: hide it in service classes
type UserMutex struct {
	mutexes map[string]*sync.Mutex
}

func NewUserMutex() *UserMutex {
	return &UserMutex{
		make(map[string]*sync.Mutex),
	}
}

func (um *UserMutex) lockForUser(u string) {
	m, ok := um.mutexes[u]
	if !ok {
		m = &sync.Mutex{}
		um.mutexes[u] = m
	}

	m.Lock()
}

func (um *UserMutex) unlockForUser(u string) {
	if m, ok := um.mutexes[u]; ok {
		m.Unlock()
	}
}

var userMutex = NewUserMutex()

// Purpose:
//
//	write passed data in DB, always treat the data as the most recent
//	doesn't change the passed data
func (h *Handler) CardSetPush(w http.ResponseWriter, r *http.Request) {
	authToken, validateSessionErr := h.sessionValidator.Validate(r)
	if validateSessionErr != nil {
		h.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode)
		return
	}

	var input Input
	err := json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		h.SetError(w, err, http.StatusBadRequest)
		return
	}

	var lastModificationDate *time.Time
	if input.LatestModificationDate != nil {
		date, err := tools.ParseApiDate(r.Context(), *input.LatestModificationDate)
		if err != nil {
			h.SetError(w, err, http.StatusBadRequest)
			return
		}
		lastModificationDate = &date
	}

	var ctxParams []any
	ctxParams = append(ctxParams, "logId", uuid.NewString())
	inputBytes, err := json.Marshal(input)
	if err != nil {
		ctxParams = append(ctxParams, "body", string(inputBytes))
	}
	ctxParams = append(ctxParams, models.LogParams(authToken, r.Header)...)

	ctx := logger.WrapContext(
		r.Context(),
		ctxParams...,
	)

	hasModifications, err := h.storage.HasModificationsSince(ctx, authToken.UserDbId, lastModificationDate)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}
	if hasModifications {
		h.SetError(w, errors.New("data has been modified, pull is required"), http.StatusConflict)
		return
	}

	// execute all the changes in one transaction
	userMutex.lockForUser(authToken.UserDbId)
	defer func() { userMutex.unlockForUser(authToken.UserDbId) }()

	response, err := h.storage.StartTransaction(ctx, func(tCtx context.Context) (interface{}, error) {
		response, sErr := h.handleUpdatedCardSets(tCtx, input.UpdatedCardSets, authToken.UserDbId)
		if sErr != nil {
			return nil, sErr
		}

		insertedCardSetIds := tools.MapValues(response.CardSetIds)
		currentCardSetIdsWithAdded := append(input.CurrentCardSetIds, insertedCardSetIds...)
		deletedIds, sErr := h.storage.IdsNotInList(tCtx, authToken.UserDbId, currentCardSetIdsWithAdded)
		if sErr != nil {
			return nil, sErr
		}

		var deleteModificationTime time.Time
		if len(deletedIds) > 0 {
			deleteModificationTime = h.TimeProvider.Now()
			sErr = h.storage.MarkAsDeletedByIds(tCtx, deletedIds, deleteModificationTime)
			if sErr != nil {
				return nil, sErr
			}
		}

		latestModificationDate, sErr := resolveLastModificationDate(ctx, lastModificationDate, input.UpdatedCardSets, deleteModificationTime)
		if sErr != nil {
			return nil, sErr
		}

		response.LatestModificationDate = tools.TimeToApiDate(latestModificationDate)
		return response, nil
	})

	if err != nil {
		switch err.(type) {
		case tools.InvalidIdError, tools.InvalidArgumentError:
			h.SetError(w, err, http.StatusBadRequest)
		default:
			h.SetError(w, err, http.StatusInternalServerError)
		}
		return
	}

	bodyAndOutputParams := []any{"isPush", true, "body", string(inputBytes)}
	outputBytes, err := json.Marshal(response)
	if err != nil {
		bodyAndOutputParams = append(bodyAndOutputParams, "output", string(outputBytes))
	}
	h.Logger.Info(ctx, "push", bodyAndOutputParams...)

	h.WriteResponse(w, response)
}

func (h *Handler) handleUpdatedCardSets(
	ctx context.Context, // transaction is required
	updatedCardSets []*api.CardSet,
	userId string,
) (*Response, error) {
	// validate
	cardWithoutIds := make(map[string]bool)

	for _, cardSet := range updatedCardSets {
		if len(cardSet.Id) == 0 && len(cardSet.CreationId) == 0 {
			return nil, tools.NewInvalidArgumentError("updatedCardSets", updatedCardSets, "card set without id has no creation id too", nil)
		}

		for _, card := range cardSet.Cards {
			if len(card.Id) == 0 {
				if len(card.CreationId) == 0 {
					return nil, tools.NewInvalidArgumentError("cardSet", cardSet, "card without id has no creation id too", nil)
				}

				cardWithoutIds[card.CreationId] = true
			}
		}
	}

	response := NewResponse()

	for _, cardSet := range updatedCardSets {
		cardSet.UserId = userId

		if len(cardSet.Id) == 0 {
			existingCardSet, err := h.storage.FindCardSetByCreationId(ctx, cardSet.CreationId)
			if err != nil {
				return nil, err
			}

			var cardSetId string
			if existingCardSet != nil {
				cardSetId = existingCardSet.Id.Hex()
				cardSet.Id = cardSetId
				err := h.storage.UpdateCardSet(ctx, cardSet)
				if err != nil {
					return nil, err
				}
			} else {
				_, err := h.storage.InsertCardSet(ctx, cardSet, userId)
				if err != nil {
					return nil, err
				}
				cardSetId = cardSet.Id
			}

			response.CardSetIds[cardSet.CreationId] = cardSetId

		} else {
			err := h.storage.UpdateCardSet(ctx, cardSet)
			if err != nil {
				return nil, err
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

func resolveLastModificationDate(ctx context.Context, pushLastModificationDate *time.Time, cardSets []*api.CardSet, deletionTime time.Time) (time.Time, error) {
	var latestModificationDate time.Time
	for _, cs := range cardSets {
		md, err := tools.ParseApiDate(ctx, cs.ModificationDate) // TODO: not optimal to parse date again
		if err != nil {
			return latestModificationDate, err
		}

		if md.Compare(latestModificationDate) > 0 {
			latestModificationDate = md
		}
	}

	if deletionTime.Compare(latestModificationDate) > 0 {
		latestModificationDate = deletionTime
	}

	if pushLastModificationDate != nil {
		if pushLastModificationDate.Compare(latestModificationDate) > 0 {
			latestModificationDate = *pushLastModificationDate
		}
	}

	return latestModificationDate, nil
}
