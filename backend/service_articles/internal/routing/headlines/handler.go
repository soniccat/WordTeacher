package headlines

import (
	"context"
	"errors"
	"models"
	"models/session_validator"
	"net/http"
	"service_articles/internal/model"
	"time"
	"tools"
	"tools/logger"

	"github.com/google/uuid"
)

const (
	limit = 100
)

type headlineStorage interface {
	FindHeadlines(
		ctx context.Context,
		category string,
		limit int64,
		since *time.Time,
	) ([]model.Headline, error)
}

type response struct {
	Headlines []model.Headline `json:"headlines"`
}

type Handler struct {
	tools.BaseHandler
	sessionValidator session_validator.SessionValidator
	headlineStorage  headlineStorage
}

func NewHandler(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionValidator session_validator.SessionValidator,
	headlineStorage headlineStorage,
) *Handler {
	return &Handler{
		BaseHandler:      *tools.NewBaseHandler(logger, timeProvider),
		sessionValidator: sessionValidator,
		headlineStorage:  headlineStorage,
	}
}

func (h *Handler) Headlines(w http.ResponseWriter, r *http.Request) {
	authToken, _ := h.sessionValidator.Validate(r) // get authToken just for logging

	// Path params
	categoryString := r.URL.Query().Get("category")
	ctx := logger.WrapContext(
		r.Context(),
		append(
			[]any{
				"logId", uuid.NewString(),
				"categoryString", categoryString,
			},
			models.LogParams(authToken, r.Header)...,
		),
	)

	headlines, err := h.headlineStorage.FindHeadlines(ctx, categoryString, limit, nil)
	if err != nil {
		var invalidIdError tools.InvalidIdError
		if errors.As(err, &invalidIdError) {
			h.SetError(w, err, http.StatusBadRequest)
		} else {
			h.SetError(w, err, http.StatusInternalServerError)
		}
		return
	}

	response := response{
		Headlines: headlines,
	}
	h.WriteResponse(w, response)
}
