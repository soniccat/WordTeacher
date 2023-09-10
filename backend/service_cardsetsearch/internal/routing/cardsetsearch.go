package routing

import (
	"api"
	"errors"
	"models/session_validator"
	"net/http"
	"service_cardsetsearch/internal/storage"

	"tools"
)

const (
	ArgumentQuery = "query"
)

type CardSetSearchResponse struct {
	CardSets []*api.CardSet `json:"cardSets,omitempty"`
}

type CardSetSearchHandler struct {
	tools.BaseHandler
	sessionValidator        session_validator.SessionValidator
	cardSetSearchRepository *storage.Repository
}

func NewCardSetSearchHandler(
	sessionValidator session_validator.SessionValidator,
	cardSetSearchRepository *storage.Repository,
) *CardSetSearchHandler {
	return &CardSetSearchHandler{
		sessionValidator:        sessionValidator,
		cardSetSearchRepository: cardSetSearchRepository,
	}
}

func (h *CardSetSearchHandler) CardSetSearch(w http.ResponseWriter, r *http.Request) {
	_, validateSessionErr := h.sessionValidator.Validate(r)
	if validateSessionErr != nil {
		h.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode)
		return
	}

	if r.Body == nil {
		h.SetError(w, errors.New("body is empty"), http.StatusBadRequest)
		return
	}

	var query = r.URL.Query().Get(ArgumentQuery)
	if len(query) == 0 {
		h.SetError(w, errors.New("query is empty"), http.StatusBadRequest)
		return
	}

	cardSets, err := h.cardSetSearchRepository.SearchCardSets(r.Context(), query)
	if err != nil {
		h.SetError(w, errors.New("query is empty"), http.StatusInternalServerError)
		return
	}

	response := CardSetSearchResponse{
		CardSets: cardSets,
	}
	h.WriteResponse(w, response)
}
