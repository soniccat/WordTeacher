package routing

import (
	"api"
	"errors"
	"net/http"
	"strings"
	"tools"
	"tools/logger"

	"github.com/google/uuid"
	"github.com/gorilla/mux"

	"service_dict/internal/wiktionary"
)

type WordResponse struct {
	Words []api.Word `json:"words"`
}

type WordHandler struct {
	tools.BaseHandler
	logger     *logger.Logger
	wiktionary wiktionary.Contract
}

func NewWordHandler(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	wiktionary wiktionary.Contract,
) *WordHandler {
	return &WordHandler{
		BaseHandler: *tools.NewBaseHandler(logger, timeProvider),
		logger:      logger,
		wiktionary:  wiktionary,
	}
}

func (h *WordHandler) Word(w http.ResponseWriter, r *http.Request) {
	params := mux.Vars(r)
	term, ok := params["term"]
	if !ok {
		h.SetError(w, errors.New("term parameter is missing"), http.StatusBadRequest)
		return
	}

	ctx := logger.WrapContext(
		r.Context(),
		"logId", uuid.NewString(),
		"term", term,
	)

	words, err := h.wiktionary.Definitions(ctx, strings.ToLower(term))
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
	}

	apiWords := tools.Map(words, func(w wiktionary.Word) api.Word {
		transcriptions := make([]string, 0, len(w.Transcriptions))
		transcriptionCount := len(w.Transcriptions)
		if transcriptionCount > 1 || transcriptionCount == 1 && w.Transcriptions[0] != "" {
			transcriptions = w.Transcriptions
		}

		apiW := api.Word{
			Term:           w.Term,
			Transcriptions: transcriptions,
			Definitions:    make(map[api.PartOfSpeech][]api.Definitions),
		}

		for k, v := range w.CatDefs {
			p := api.PartOfSpeechFromString(k)

			_, ok := apiW.Definitions[p]
			if !ok {
				apiW.Definitions[p] = make([]api.Definitions, 0, len(v))
			}

			for _, wiktionaryDef := range v {
				apiW.Definitions[p] = append(apiW.Definitions[p], api.Definitions{
					Definitions: []string{wiktionaryDef.Def},
					Examples:    removeTermSurroundings(wiktionaryDef.Examples),
					Synonyms:    removeTermSurroundings(wiktionaryDef.Synonyms),
					Antonyms:    removeTermSurroundings(wiktionaryDef.Antonyms),
				})
			}
		}

		return apiW
	})
	response := WordResponse{
		Words: apiWords,
	}

	h.WriteResponse(w, response)
}

func removeTermSurroundings(slice []string) []string {
	return tools.Map(slice, func(ex string) string {
		return strings.ReplaceAll(ex, "'''", "")
	})
}
