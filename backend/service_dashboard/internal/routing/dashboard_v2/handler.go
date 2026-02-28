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
	TagWithCardSets() []api.TagWithCardSets
}

type response struct {
	HeadlineBlock        model.DashboardHeadlineBlock        `json:"headlineBlock"`
	NewCardSetBlock      model.DashboardNewCardsSetBlock     `json:"newCardSetBlock"`
	TagWithCardSetsBlock model.DashboardTagWithCardSetsBlock `json:"tagWithCardSetsBlock"`
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

	response := response{
		HeadlineBlock: model.DashboardHeadlineBlock{
			Categories: h.headlineStorage.HeadlineCategories(),
		},
		NewCardSetBlock: model.DashboardNewCardsSetBlock{
			CardSets: h.cardSetsStorage.CardSets(),
		},
		TagWithCardSetsBlock: model.DashboardTagWithCardSetsBlock{
			Tags: h.cardSetsStorage.TagWithCardSets(),
		},
	}
	h.WriteResponse(w, response)
}
