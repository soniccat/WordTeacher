package cardset_by_id

import (
	"api"
	"context"
	"models"
	"net/http"
	"time"
	"tools"
	"tools/logger"

	"github.com/google/uuid"
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
	authToken, _ := h.sessionValidator.Validate(r) // get authToken just for logging

	// Path params
	params := mux.Vars(r)
	cardSetId, ok := params["id"]
	if !ok {
		h.SetError(w, errors.New("id parameter is missing"), http.StatusBadRequest)
		return
	}

	ctx := logger.WrapContext(
		r.Context(),
		append(
			[]any{
				"logId", uuid.NewString(),
				"cardSetId", cardSetId,
			},
			models.LogParams(authToken, r.Header)...,
		),
	)

	dbCardSet, err := h.innerStorage.LoadCardSetDbById(ctx, cardSetId)
	if err != nil {
		var invalidIdError tools.InvalidIdError
		if errors.As(err, &invalidIdError) {
			h.SetError(w, err, http.StatusBadRequest)
		} else {
			h.SetError(w, err, http.StatusServiceUnavailable)
		}
		return
	}

	// return a copy:
	// * cut progress data
	// * clear cardset id and its card ids
	// * clear modification date
	// * set isAvailableInSearch in false
	defaultCardProgress := &api.CardProgress{
		CurrentLevel:     0,
		LastMistakeCount: 0,
		LastLessonDate:   "",
	}
	date := tools.TimeToApiDate(time.Now())
	dbCardSet.Cards = tools.Map(dbCardSet.Cards, func(c *model.DbCard) *model.DbCard {
		c.Id = nil
		c.Progress = defaultCardProgress
		c.CreationDate = date
		c.ModificationDate = date
		return c
	})

	apiCardSet := dbCardSet.ToApi()
	apiCardSet.Id = ""
	apiCardSet.CreationDate = date
	apiCardSet.CreationId = uuid.NewString()
	apiCardSet.ModificationDate = date
	apiCardSet.UserId = ""
	apiCardSet.IsAvailableInSearch = false

	response := response{
		CardSet: apiCardSet,
	}
	h.WriteResponse(w, response)
}
