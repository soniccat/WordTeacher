package files

import (
	"api/api_dict"
	"models/session_validator"
	"net/http"
	"tools"
	"tools/logger"
)

type Handler struct {
	tools.BaseHandler
	logger           *logger.Logger
	sessionValidator session_validator.SessionValidator
}

func NewHandler(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionValidator session_validator.SessionValidator,
) *Handler {
	return &Handler{
		BaseHandler:      *tools.NewBaseHandler(logger, timeProvider),
		logger:           logger,
		sessionValidator: sessionValidator,
	}
}

func (h *Handler) Files(w http.ResponseWriter, r *http.Request) {
	// authToken, _ := h.sessionValidator.Validate(r) // get authToken just for logging

	// ctx := logger.WrapContext(
	// 	r.Context(),
	// 	append(
	// 		[]any{
	// 			"logId", uuid.NewString(),
	// 		},
	// 		models.LogParams(authToken, r.Header)...,
	// 	),
	// )

	response := api_dict.Files{
		Dicts: []api_dict.Dict{
			{
				Name:             "Wiktionary EN-EN Core",
				Description:      "Common Wiktionary words and phrases",
				Url:              "https://aglushkov.ru/dicts/Wiktionary_EN-EN_Core.dsl.gz",
				FromLang:         "EN",
				ToLang:           "EN",
				Size:             19854421,
				ModificationDate: "2026-11-05T12:34:02Z",
			},
		},
	}

	h.WriteResponse(w, response)
}
