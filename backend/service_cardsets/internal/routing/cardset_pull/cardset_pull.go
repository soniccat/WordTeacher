package cardset_pull

import (
	"api"
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"time"
	"tools"
	"tools/logger"

	mapset "github.com/deckarep/golang-set/v2"

	"models/session_validator"
	"service_cardsets/internal/model"
	"service_cardsets/internal/routing"
)

type storage interface {
	CardCardSetIds(ctx context.Context, userId string) ([]string, error)
	ModifiedCardSetsSince(ctx context.Context, userId *string, lastModificationDate *time.Time) ([]*model.DbCardSet, error)
}

type Input struct {
	CurrentCardSetIds []string `json:"currentCardSetIds,omitempty"`
}

type Response struct {
	UpdatedCardSets   []*api.CardSet `json:"updatedCardSets,omitempty"`
	DeletedCardSetIds []string       `json:"deletedCardSetIds,omitempty"`
}

type Handler struct {
	tools.BaseHandler
	sessionValidator session_validator.SessionValidator
	storage          storage
}

func NewHandler(
	logger *logger.Logger,
	sessionValidator session_validator.SessionValidator,
	storage storage,
) *Handler {
	return &Handler{
		BaseHandler:      *tools.NewBaseHandler(logger),
		sessionValidator: sessionValidator,
		storage:          storage,
	}
}

func (h *Handler) CardSetPull(w http.ResponseWriter, r *http.Request) {
	authToken, validateSessionErr := h.sessionValidator.Validate(r)
	if validateSessionErr != nil {
		h.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode)
		return
	}

	if r.Body == nil {
		h.SetError(w, errors.New("body is empty"), http.StatusBadRequest)
		return
	}

	var input Input
	err := json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		h.SetError(w, err, http.StatusBadRequest)
		return
	}

	var lastModificationDate *time.Time
	if parsedDate, err := time.Parse(time.RFC3339, r.URL.Query().Get(routing.ParameterLatestCardSetModificationDate)); err == nil {
		lastModificationDate = &parsedDate
	}

	ctx := r.Context()

	dbCardSets, err := h.storage.ModifiedCardSetsSince(ctx, &authToken.UserDbId, lastModificationDate)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}

	apiCardSets := model.DbCardSetsToApi(dbCardSets)
	idsToDelete, handlerErr := h.resolveDeletedCardIds(ctx, authToken.UserDbId, &input)
	if handlerErr != nil {
		h.SetHandlerError(w, handlerErr)
		return
	}

	response := Response{
		UpdatedCardSets:   apiCardSets,
		DeletedCardSetIds: idsToDelete,
	}
	h.WriteResponse(w, response)
}

func (h *Handler) resolveDeletedCardIds(
	ctx context.Context,
	userId string,
	input *Input,
) ([]string, *tools.HandlerError) {
	userCardSetIds, err := h.storage.CardCardSetIds(ctx, userId)
	if err != nil {
		return nil, h.NewHandlerError(http.StatusInternalServerError, err)
	}

	currentCardSetIdSet := mapset.NewSet(input.CurrentCardSetIds...)
	for i := range userCardSetIds {
		currentCardSetIdSet.Remove(userCardSetIds[i])
	}

	return currentCardSetIdSet.ToSlice(), nil
}
