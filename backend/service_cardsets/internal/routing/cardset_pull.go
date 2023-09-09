package routing

import (
	"api"
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"time"
	"tools"

	mapset "github.com/deckarep/golang-set/v2"
	"go.mongodb.org/mongo-driver/bson/primitive"

	"models/session_validator"
	"service_cardsets/internal/model"
	"service_cardsets/internal/storage"
)

type CardSetPullInput struct {
	CurrentCardSetIds []string `json:"currentCardSetIds,omitempty"`
}

type CardSetPullResponse struct {
	UpdatedCardSets   []*api.CardSet `json:"updatedCardSets,omitempty"`
	DeletedCardSetIds []string       `json:"deletedCardSetIds,omitempty"`
}

type CardSetPullHandler struct {
	tools.BaseHandler
	sessionValidator  session_validator.SessionValidator
	cardSetRepository *storage.Repository
}

func NewCardSetPullHandler(
	sessionValidator session_validator.SessionValidator,
	cardSetRepository *storage.Repository,
) *CardSetPullHandler {
	return &CardSetPullHandler{
		sessionValidator:  sessionValidator,
		cardSetRepository: cardSetRepository,
	}
}

func (h *CardSetPullHandler) CardSetPull(w http.ResponseWriter, r *http.Request) {
	authToken, validateSessionErr := h.sessionValidator.Validate(r)
	if validateSessionErr != nil {
		h.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode)
		return
	}

	if r.Body == nil {
		h.SetError(w, errors.New("body is empty"), http.StatusBadRequest)
		return
	}

	var input CardSetPullInput
	err := json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		h.SetError(w, err, http.StatusBadRequest)
		return
	}

	var lastModificationDate *time.Time
	if parsedDate, err := time.Parse(time.RFC3339, r.URL.Query().Get(ParameterLatestCardSetModificationDate)); err == nil {
		lastModificationDate = &parsedDate
	}

	ctx := r.Context()

	dbCardSets, err := h.cardSetRepository.ModifiedCardSetsSince(ctx, authToken.UserMongoId, lastModificationDate)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}

	apiCardSets := model.DbCardSetsToApi(dbCardSets)
	idsToDelete, handlerErr := h.resolveDeletedCardIds(ctx, authToken.UserMongoId, &input)
	if handlerErr != nil {
		h.SetHandlerError(w, handlerErr)
		return
	}

	response := CardSetPullResponse{
		UpdatedCardSets:   apiCardSets,
		DeletedCardSetIds: idsToDelete,
	}
	h.WriteResponse(w, response)
}

func (h *CardSetPullHandler) resolveDeletedCardIds(
	ctx context.Context,
	userId *primitive.ObjectID,
	input *CardSetPullInput,
) ([]string, *tools.HandlerError) {
	userCardSetIds, err := h.cardSetRepository.CardCardSetIds(ctx, userId)
	if err != nil {
		return nil, h.NewHandlerError(http.StatusInternalServerError, err)
	}

	currentCardSetIdSet := mapset.NewSet(input.CurrentCardSetIds...)
	for i := range userCardSetIds {
		currentCardSetIdSet.Remove(userCardSetIds[i])
	}

	return currentCardSetIdSet.ToSlice(), nil
}
