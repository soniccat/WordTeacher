package dashboard

import (
	"models/session_validator"
	"net/http"
	"service_dashboard/internal/model"
	"tools"
	"tools/logger"
)

type headlineStorage interface {
	HeadlineCategories() []model.DashboardHeadlineCategory
}

type response struct {
	HeadlineBlock model.DashboardHeadlineBlock `json:"headlineBlock"`
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

	response := response{
		HeadlineBlock: model.DashboardHeadlineBlock{
			Categories: headlineCategories,
		},
	}
	h.WriteResponse(w, response)
}
