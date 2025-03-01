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
	"github.com/gorilla/mux"
)

const (
	limit = 100
)

type storage interface {
	FindHeadlines(
		ctx context.Context,
		since time.Time,
		limit int64,
	) ([]model.Headline, error)
}

type response struct {
	Headlines []model.Headline `json:"headlines"`
}

type Handler struct {
	tools.BaseHandler
	sessionValidator session_validator.SessionValidator
	innerStorage     storage
}

func NewHandler(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionValidator session_validator.SessionValidator,
	innerStorage storage,
) *Handler {
	return &Handler{
		BaseHandler:      *tools.NewBaseHandler(logger, timeProvider),
		sessionValidator: sessionValidator,
		innerStorage:     innerStorage,
	}
}

func (h *Handler) Headlines(w http.ResponseWriter, r *http.Request) {
	authToken, _ := h.sessionValidator.Validate(r) // get authToken just for logging

	// Path params
	params := mux.Vars(r)
	sinceString, ok := params["since"]
	if !ok {
		h.SetError(w, errors.New("since parameter is missing"), http.StatusBadRequest)
		return
	}

	sinceDate, err := tools.ParseApiDate(r.Context(), sinceString)
	if err != nil {
		h.SetError(w, errors.New("since parameter is a wrong date"), http.StatusBadRequest)
		return
	}

	ctx := logger.WrapContext(
		r.Context(),
		append(
			[]any{
				"logId", uuid.NewString(),
				"since", sinceString,
			},
			models.LogParams(authToken, r.Header)...,
		),
	)

	headlines, err := h.innerStorage.FindHeadlines(ctx, sinceDate, limit)
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
