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
				Size:             19854432,
				ModificationDate: "2026-11-15T07:23:26Z",
			},
			{
				Name:             "WT Wiktionary EN-EN Phrasal Verbs and Idioms",
				Description:      "Wiktionary phrasal verbs and idioms",
				Url:              "https://aglushkov.ru/dicts/Wiktionary_EN-EN_Phrasal_Verbs_and_Idioms.dsl.gz",
				FromLang:         "EN",
				ToLang:           "EN",
				Size:             1472399,
				ModificationDate: "2026-11-20T12:24:44Z",
			},
			{
				Name:             "WT Wiktionary EN-EN Animals",
				Description:      "Mammals, birds, fish, insects",
				Url:              "https://aglushkov.ru/dicts/Wiktionary_EN-EN_animals.dsl.gz",
				FromLang:         "EN",
				ToLang:           "EN",
				Size:             916282,
				ModificationDate: "2026-11-20T16:45:30Z",
			},
			{
				Name:             "WT Wiktionary EN-EN Arts Entertainment",
				Description:      "Sports, music, art, language, fashion",
				Url:              "https://aglushkov.ru/dicts/Wiktionary_EN-EN_arts_entertainment.dsl.gz",
				FromLang:         "EN",
				ToLang:           "EN",
				Size:             1151969,
				ModificationDate: "2026-11-20T16:48:46Z",
			},
			{
				Name:             "WT Wiktionary EN-EN Food and Home",
				Description:      "Food, household, buildings, transport, calendar",
				Url:              "https://aglushkov.ru/dicts/Wiktionary_EN-EN_food_home.dsl.gz",
				FromLang:         "EN",
				ToLang:           "EN",
				Size:             1592522,
				ModificationDate: "2026-11-20T16:48:46Z",
			},
			{
				Name:             "WT Wiktionary EN-EN Health and Body",
				Description:      "Medicine, anatomy, emotions",
				Url:              "https://aglushkov.ru/dicts/Wiktionary_EN-EN_health_body.dsl.gz",
				FromLang:         "EN",
				ToLang:           "EN",
				Size:             696856,
				ModificationDate: "2026-11-20T16:48:46Z",
			},
			{
				Name:             "WT Wiktionary EN-EN History and Society",
				Description:      "History, war, religion, politics, law, finance, work, education",
				Url:              "https://aglushkov.ru/dicts/Wiktionary_EN-EN_history_society.dsl.gz",
				FromLang:         "EN",
				ToLang:           "EN",
				Size:             1613345,
				ModificationDate: "2026-11-20T16:48:46Z",
			},
			{
				Name:             "WT Wiktionary EN-EN Nature Geography",
				Description:      "Plants, weather, places",
				Url:              "https://aglushkov.ru/dicts/Wiktionary_EN-EN_nature_geography.dsl.gz",
				FromLang:         "EN",
				ToLang:           "EN",
				Size:             613151,
				ModificationDate: "2026-11-20T16:48:46Z",
			},
			{
				Name:             "WT Wiktionary EN-EN Science and Tech",
				Description:      "Chemistry, math, science, astronomy, computing",
				Url:              "https://aglushkov.ru/dicts/Wiktionary_EN-EN_science_tech.dsl.gz",
				FromLang:         "EN",
				ToLang:           "EN",
				Size:             1165423,
				ModificationDate: "2026-11-20T16:48:46Z",
			},
		},
	}

	h.WriteResponse(w, response)
}
