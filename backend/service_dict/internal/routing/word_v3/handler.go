package word_v3

import (
	"api"
	api_dict_v2 "api/api_dict/v2"
	"context"
	"errors"
	"models"
	"models/session_validator"
	"net/http"
	"strings"
	"tools"
	"tools/logger"

	"github.com/google/uuid"

	"service_dict/internal/wiktionary/repository_v2"
)

type response struct {
	Words []api_dict_v2.Word `json:"words"`
}

type repository interface {
	DefinitionsForTerms(ctx context.Context, terms []string) ([]repository_v2.WordEntry, error)
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

func (h *Handler) Word(w http.ResponseWriter, r *http.Request) {
	authToken, _ := h.sessionValidator.Validate(r) // get authToken just for logging

	termsString := r.URL.Query().Get("terms")
	terms := strings.Split(termsString, ",")
	if len(terms) == 0 {
		h.SetError(w, errors.New("no terms"), http.StatusBadRequest)
		return
	}

	ctx := logger.WrapContext(
		r.Context(),
		append(
			[]any{
				"logId", uuid.NewString(),
				"terms", termsString,
			},
			models.LogParams(authToken, r.Header)...,
		),
	)

	words, err := h.repository.DefinitionsForTerms(ctx, terms)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
	}

	apiWords := tools.Map(words, func(w repository_v2.WordEntry) api_dict_v2.Word {
		apiW := api_dict_v2.Word{
			Term:           w.Term,
			Transcriptions: w.Transcriptions,
			AudioFiles: tools.Map(w.Audios, func(wa repository_v2.WordAudio) api_dict_v2.WordAudioFile {
				return api_dict_v2.WordAudioFile{
					Url:           "https://aglushkov.ru/" + wa.FileName,
					Accent:        wa.Accent,
					Transcription: wa.Transcription,
					Text:          wa.Text,
				}
			}),
		}

		for _, v := range w.DefPairs {
			p := api.PartOfSpeechFromString(v.PartOfSpeech)
			apiW.DefPairs = append(apiW.DefPairs, api_dict_v2.DefPair{
				PartOfSpeech: p,
				DefEntries: tools.Map(
					v.DefEntries,
					func(e repository_v2.WordDefEntry) api_dict_v2.DefEntry {
						return api_dict_v2.DefEntry{
							Definition: api_dict_v2.Definition{
								Value:  e.Def.Value,
								Labels: e.Def.Labels,
							},
							Examples: e.Examples,
							Synonyms: e.Synonyms,
							Antonyms: e.Antonyms,
						}
					},
				),
			})
		}

		return apiW
	})

	response := response{
		Words: apiWords,
	}

	h.WriteResponse(w, response)
}
