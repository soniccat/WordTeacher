package text_search

import (
	"api"
	api_dict_v2 "api/api_dict/v2"
	"context"
	"errors"
	"models"
	"models/session_validator"
	"net/http"
	"slices"
	"strings"
	"tools"
	"tools/logger"

	"github.com/google/uuid"
	"github.com/gorilla/mux"

	"service_dict/internal/wiktionary/repository_v2"
)

type response struct {
	Words []api_dict_v2.Word `json:"words"`
}

type repository interface {
	WordExamples(ctx context.Context, text string, limit int) ([]repository_v2.WordExamples, error)
}

type Handler struct {
	tools.BaseHandler
	logger           *logger.Logger
	sessionValidator session_validator.SessionValidator
	repository       repository
}

func NewHandler(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionValidator session_validator.SessionValidator,
	repository repository,
) *Handler {
	return &Handler{
		BaseHandler:      *tools.NewBaseHandler(logger, timeProvider),
		logger:           logger,
		sessionValidator: sessionValidator,
		repository:       repository,
	}
}

func (h *Handler) WordTextSearch(w http.ResponseWriter, r *http.Request) {
	authToken, _ := h.sessionValidator.Validate(r) // get authToken just for logging

	params := mux.Vars(r)
	text, ok := params["text"]
	if !ok {
		h.SetError(w, errors.New("term parameter is missing"), http.StatusBadRequest)
		return
	}

	ctx := logger.WrapContext(
		r.Context(),
		append(
			[]any{
				"logId", uuid.NewString(),
				"text", text,
			},
			models.LogParams(authToken, r.Header)...,
		),
	)

	words, err := h.repository.WordExamples(ctx, strings.ToLower(text), 20)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
	}

	textSearchEntries := tools.Map(words, func(w repository_v2.WordExamples) api_dict_v2.Word {
		apiDefPairs := []api_dict_v2.DefPair{}
		for defPairIndex, defPair := range w.Word.DefPairs {

			apiDefPair := api_dict_v2.DefPair{}
			for defEntryIndex, defEntry := range defPair.DefEntries {

				apiDefEntry := api_dict_v2.DefEntry{}
				for exampleIndex, example := range defEntry.Examples {
					keep := slices.ContainsFunc(
						w.Examples,
						func(e repository_v2.WordExample) bool {
							return e.ExampleIndex == exampleIndex &&
								e.DefEntryIndex == defEntryIndex &&
								e.DefPairIndex == defPairIndex
						},
					)

					if keep {
						apiDefEntry.Examples = append(apiDefEntry.Examples, example)
					}
				}

				if len(apiDefEntry.Examples) > 0 {
					apiDefEntry.Antonyms = defEntry.Antonyms
					apiDefEntry.Definition = api_dict_v2.Definition{
						Value:  defEntry.Def.Value,
						Labels: defEntry.Def.Labels,
					}
					apiDefEntry.Synonyms = defEntry.Synonyms

					apiDefPair.DefEntries = append(apiDefPair.DefEntries, apiDefEntry)
					apiDefPair.PartOfSpeech = api.PartOfSpeechFromString(defPair.PartOfSpeech)
				}
			}

			if len(apiDefPair.DefEntries) > 0 {
				apiDefPairs = append(apiDefPairs, apiDefPair)
			}
		}

		return api_dict_v2.Word{
			Term:           w.Word.Term,
			Transcriptions: w.Word.Transcriptions,
			DefPairs:       apiDefPairs,
		}
	})

	response := response{
		Words: textSearchEntries,
	}

	h.WriteResponse(w, response)
}
