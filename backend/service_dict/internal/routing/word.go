package routing

import (
	"api"
	"errors"
	"net/http"
	"tools"
	"tools/logger"

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
	wiktionary wiktionary.Contract,
) *WordHandler {
	return &WordHandler{
		BaseHandler: *tools.NewBaseHandler(logger),
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

	words, err := h.wiktionary.Definitions(r.Context(), term)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
	}

	apiWords := tools.Map(words, func(w wiktionary.Word) api.Word {
		apiW := api.Word{
			Term:           w.Term,
			Transcriptions: w.Transcriptions,
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
					Examples:    wiktionaryDef.Examples,
					Synonyms:    wiktionaryDef.Synonyms,
					Antonyms:    wiktionaryDef.Antonyms,
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
