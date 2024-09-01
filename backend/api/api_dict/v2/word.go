package api_dict_v2

import (
	"api"
)

type Word struct {
	Term           string    `json:"term"`
	Transcriptions []string  `json:"transcriptions,omitempty"`
	DefPairs       []DefPair `json:"defpairs"`
}

type DefPair struct {
	PartOfSpeech api.PartOfSpeech `bson:"partOfSpeech,omitempty"`
	Definitions  []Definitions    `bson:"definitions,omitempty"`
}

type Definitions struct {
	Definition Definition `json:"definition"`
	Examples   []string   `json:"examples,omitempty"`
	Synonyms   []string   `json:"synonyms,omitempty"`
	Antonyms   []string   `json:"antonyms,omitempty"`
}

type Definition struct {
	Value  string   `json:"value"`
	Labels []string `json:"labels,omitempty"`
}
