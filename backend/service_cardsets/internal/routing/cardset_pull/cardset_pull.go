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
	"github.com/google/uuid"
	"go.mongodb.org/mongo-driver/bson/primitive"

	"models/session_validator"
	"service_cardsets/internal/model"
)

type storage interface {
	CardCardSetIds(ctx context.Context, userId string) ([]string, error)
	ModifiedCardSetsSinceByUserId(ctx context.Context, userId string, lastModificationDate *time.Time) ([]*model.DbCardSet, error)
	LastModificationDate(ctx context.Context, userId string) (*time.Time, error)
}

type Input struct {
	CurrentCardSetIds      []string `json:"currentCardSetIds,omitempty"`
	LatestModificationDate *string  `json:"latestModificationDate,omitempty"`
}

type Response struct {
	UpdatedCardSets        []*api.CardSet `json:"updatedCardSets,omitempty"`
	DeletedCardSetIds      []string       `json:"deletedCardSetIds,omitempty"`
	LatestModificationDate string         `json:"latestModificationDate"`
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

	ctx := logger.WrapContext(
		r.Context(),
		ctxParams...,
	)

	dbCardSets, err := h.storage.ModifiedCardSetsSinceByUserId(ctx, authToken.UserDbId, lastModificationDate)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}

	apiCardSets := model.DbCardSetsToApi(dbCardSets)
	idsToDelete, err := h.resolveDeletedCardIds(ctx, authToken.UserDbId, &input)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}

	lastCardSetModificationDate, err := h.calcLastModificationDate(ctx, authToken.UserDbId, lastModificationDate, dbCardSets)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}

	response := Response{
		UpdatedCardSets:        apiCardSets,
		DeletedCardSetIds:      idsToDelete,
		LatestModificationDate: lastCardSetModificationDate,
	}
	h.WriteResponse(w, response)
}

func (h *Handler) calcLastModificationDate(
	ctx context.Context,
	userId string,
	pullLastModificationDate *time.Time,
	dbCardSets []*model.DbCardSet,
) (string, error) {
	var lastMongoModificationDate primitive.DateTime
	for _, c := range dbCardSets {
		if c.ModificationDate > lastMongoModificationDate {
			lastMongoModificationDate = c.ModificationDate
		}
	}

	lastCardSetModificationDate := lastMongoModificationDate.Time()
	lastDateInDb, err := h.storage.LastModificationDate(ctx, userId)
	if err != nil {
		return "", err
	}

	if lastDateInDb != nil {
		if lastDateInDb.Compare(lastCardSetModificationDate) > 0 {
			lastCardSetModificationDate = *lastDateInDb
		}
	}

	if pullLastModificationDate != nil {
		if pullLastModificationDate.Compare(lastCardSetModificationDate) > 0 {
			lastCardSetModificationDate = *pullLastModificationDate
		}
	}

	return tools.TimeToApiDate(lastCardSetModificationDate), nil
}

func (h *Handler) resolveDeletedCardIds(
	ctx context.Context,
	userId string,
	input *Input,
) ([]string, error) {
	userCardSetIds, err := h.storage.CardCardSetIds(ctx, userId)
	if err != nil {
		return nil, err
	}

	currentCardSetIdSet := mapset.NewSet(input.CurrentCardSetIds...)
	for i := range userCardSetIds {
		currentCardSetIdSet.Remove(userCardSetIds[i])
	}

	return currentCardSetIdSet.ToSlice(), nil
}
