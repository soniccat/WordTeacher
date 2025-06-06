package dashboard

import (
	"api"
	"models/session_validator"
	"net/http"
	"service_dashboard/internal/model"
	"tools"
	"tools/logger"
)

type headlineStorage interface {
	HeadlineCategories() []model.DashboardHeadlineCategory
}

type cardSetsStorage interface {
	CardSets() []api.CardSet
}

type response struct {
	HeadlineBlock   model.DashboardHeadlineBlock    `json:"headlineBlock"`
	NewCardSetBlock model.DashboardNewCardsSetBlock `json:"newCardSetBlock"`
}

type Handler struct {
	tools.BaseHandler
	sessionValidator session_validator.SessionValidator
	headlineStorage  headlineStorage
	cardSetsStorage  cardSetsStorage
}

func New(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionValidator session_validator.SessionValidator,
	headlineStorage headlineStorage,
	cardSetsStorage cardSetsStorage,
) *Handler {
	return &Handler{
		BaseHandler:      *tools.NewBaseHandler(logger, timeProvider),
		sessionValidator: sessionValidator,
		headlineStorage:  headlineStorage,
		cardSetsStorage:  cardSetsStorage,
	}
}

func (h *Handler) Handle(w http.ResponseWriter, r *http.Request) {
	// authToken, _ := h.sessionValidator.Validate(r) // get authToken just for logging

	// // Path params
	// ctx := logger.WrapContext(
	// 	r.Context(),
	// 	append(
	// 		[]any{
	// 			"logId", uuid.NewString(),
	// 			"categoryString", categoryString,
	// 		},
	// 		models.LogParams(authToken, r.Header)...,
	// 	),
	// )

	headlineCategories := h.headlineStorage.HeadlineCategories()
	newCardSets := h.cardSetsStorage.CardSets()

	response := response{
		HeadlineBlock: model.DashboardHeadlineBlock{
			Categories: headlineCategories,
		},
		NewCardSetBlock: model.DashboardNewCardsSetBlock{
			CardSets: newCardSets,
		},
	}
	h.WriteResponse(w, response)
}
