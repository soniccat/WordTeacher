package routing

import (
	"api"
	"net/http"
	"tools"

	"github.com/gorilla/mux"
	"github.com/pkg/errors"
	"go.mongodb.org/mongo-driver/bson/primitive"

	"models/session_validator"
	"service_cardsets/internal/model"
	"service_cardsets/internal/storage"
)

type CardSetGetByIdResponse struct {
	CardSet *api.CardSet `json:"cardSet"`
}

type CardSetByIdHandler struct {
	tools.BaseHandler
	sessionValidator  session_validator.SessionValidator
	cardSetRepository *storage.Repository
}

func NewCardSetByIdHandler(
	sessionValidator session_validator.SessionValidator,
	cardSetRepository *storage.Repository,
) *CardSetByIdHandler {
	return &CardSetByIdHandler{
		sessionValidator:  sessionValidator,
		cardSetRepository: cardSetRepository,
	}
}

func (h *CardSetByIdHandler) CardSetById(w http.ResponseWriter, r *http.Request) {
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

	cardSetDbId, err := primitive.ObjectIDFromHex(cardSetId)
	if err != nil {
		h.SetError(w, err, http.StatusBadRequest)
		return
	}

	dbCardSet, err := h.cardSetRepository.LoadCardSetDbByObjectID(r.Context(), cardSetDbId)
	if err != nil {
		h.SetError(w, err, http.StatusServiceUnavailable)
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
	response := CardSetGetByIdResponse{
		CardSet: apiCardSet,
	}
	h.WriteResponse(w, response)
}
