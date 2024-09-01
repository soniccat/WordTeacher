package api_dict_v1

import (
	"api"
)

type Word struct {
	Term           string                             `json:"term"`
	Transcriptions []string                           `json:"transcriptions,omitempty"`
	Definitions    map[api.PartOfSpeech][]Definitions `json:"definitions"`
}

type Definitions struct {
	Definitions []string `json:"definitions"`
	Examples    []string `json:"examples,omitempty"`
	Synonyms    []string `json:"synonyms,omitempty"`
	Antonyms    []string `json:"antonyms,omitempty"`
}
