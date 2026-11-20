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
				Name:             "WT Wiktionary EN-EN Core",
				Description:      "Common Wiktionary words and phrases",
				Url:              "https://aglushkov.ru/dicts/Wiktionary_EN-EN_Core.dsl.gz",
				FromLang:         "EN",
				ToLang:           "EN",
				Size:             19854421,
				ModificationDate: "2026-11-15T07:23:26Z",
			},
			{
				Name:             "WT Wiktionary EN-EN Phrasal Verbs and Idioms",
				Description:      "Wiktionary phrasal verbs and idioms",
				Url:              "https://aglushkov.ru/dicts/Wiktionary_EN-EN_Phrasal_Verbs_and_Idioms.dsl.gz",
				FromLang:         "EN",
				ToLang:           "EN",
				Size:             1329354,
				ModificationDate: "2026-11-20T12:24:44Z",
			},
		},
	}

	h.WriteResponse(w, response)
}
