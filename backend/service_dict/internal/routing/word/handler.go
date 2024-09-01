package word

import (
	"api"
	api_dict_v1 "api/api_dict/v1"
	"context"
	"errors"
	"models"
	"models/session_validator"
	"net/http"
	rep "service_dict/internal/wiktionary/repository"
	"strings"
	"tools"
	"tools/logger"

	"github.com/google/uuid"
	"github.com/gorilla/mux"
)

type response struct {
	Words []api_dict_v1.Word `json:"words"`
}

type repository interface {
	Definitions(ctx context.Context, term string) ([]rep.Word, error)
	CreateIndexIfNeeded(ctx context.Context) error
}

type Handler struct {
	tools.BaseHandler
	logger           *logger.Logger
	sessionValidator session_validator.SessionValidator
	wiktionary       repository
}

func NewHandler(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionValidator session_validator.SessionValidator,
	wiktionary repository,
) *Handler {
	return &Handler{
		BaseHandler:      *tools.NewBaseHandler(logger, timeProvider),
		logger:           logger,
		sessionValidator: sessionValidator,
		wiktionary:       wiktionary,
	}
}

func (h *Handler) Word(w http.ResponseWriter, r *http.Request) {
	authToken, _ := h.sessionValidator.Validate(r) // get authToken just for logging

	params := mux.Vars(r)
	term, ok := params["term"]
	if !ok {
		h.SetError(w, errors.New("term parameter is missing"), http.StatusBadRequest)
		return
	}

	ctx := logger.WrapContext(
		r.Context(),
		append(
			[]any{
				"logId", uuid.NewString(),
				"term", term,
			},
			models.LogParams(authToken, r.Header)...,
		),
	)

	words, err := h.wiktionary.Definitions(ctx, strings.ToLower(term))
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
	}

	apiWords := tools.Map(words, func(w rep.Word) api_dict_v1.Word {
		transcriptions := make([]string, 0, len(w.Transcriptions))
		transcriptionCount := len(w.Transcriptions)
		if transcriptionCount > 1 || transcriptionCount == 1 && w.Transcriptions[0] != "" {
			transcriptions = w.Transcriptions
		}

		apiW := api_dict_v1.Word{
			Term:           w.Term,
			Transcriptions: transcriptions,
			Definitions:    make(map[api.PartOfSpeech][]api_dict_v1.Definitions),
		}

		for k, v := range w.CatDefs {
			p := api.PartOfSpeechFromString(k)

			_, ok := apiW.Definitions[p]
			if !ok {
				apiW.Definitions[p] = make([]api_dict_v1.Definitions, 0, len(v))
			}

			for _, wiktionaryDef := range v {
				apiW.Definitions[p] = append(apiW.Definitions[p], api_dict_v1.Definitions{
					Definitions: []string{wiktionaryDef.Def},
					Examples:    removeTermSurroundings(wiktionaryDef.Examples),
					Synonyms:    removeTermSurroundings(wiktionaryDef.Synonyms),
					Antonyms:    removeTermSurroundings(wiktionaryDef.Antonyms),
				})
			}
		}

		return apiW
	})
	response := response{
		Words: apiWords,
	}

	h.WriteResponse(w, response)
}

func removeTermSurroundings(slice []string) []string {
	return tools.Map(slice, func(ex string) string {
		return strings.ReplaceAll(ex, "'''", "")
	})
}
