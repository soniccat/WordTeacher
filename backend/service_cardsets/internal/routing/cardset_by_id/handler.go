package cardset_by_id

import (
	"api"
	"context"
	"net/http"
	"tools"
	"tools/logger"

	"github.com/gorilla/mux"
	"github.com/pkg/errors"

	"models/session_validator"
	"service_cardsets/internal/model"
)

type storage interface {
	LoadCardSetDbById(
		ctx context.Context,
		id string,
	) (*model.DbCardSet, error)
}

type response struct {
	CardSet *api.CardSet `json:"cardSet"`
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

func (h *Handler) CardSetById(w http.ResponseWriter, r *http.Request) {
	_, validateSessionErr := h.sessionValidator.Validate(r)
	if validateSessionErr != nil {
		h.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode)
		return
	}

	// Path params
	params := mux.Vars(r)
	cardSetId, ok := params["id"]
	if !ok {
		h.SetError(w, errors.New("id parameter is missing"), http.StatusBadRequest)
		return
	}

	dbCardSet, err := h.innerStorage.LoadCardSetDbById(r.Context(), cardSetId)
	if err != nil {
		var invalidIdError tools.InvalidIdError
		if errors.As(err, &invalidIdError) {
			h.SetError(w, err, http.StatusBadRequest)
		} else {
			h.SetError(w, err, http.StatusServiceUnavailable)
		}
		return
	}

	// cut progress data
	defaultCardProgress := &api.CardProgress{
		CurrentLevel:     0,
		LastMistakeCount: 0,
		LastLessonDate:   "",
	}
	dbCardSet.Cards = tools.Map(dbCardSet.Cards, func(c *model.DbCard) *model.DbCard {
		c.Progress = defaultCardProgress
		return c
	})

	apiCardSet := dbCardSet.ToApi()
	response := response{
		CardSet: apiCardSet,
	}
	h.WriteResponse(w, response)
}
