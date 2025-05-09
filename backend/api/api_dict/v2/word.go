package api_dict_v2

import (
	"api"
)

type Word struct {
	Term           string          `json:"term"`
	Transcriptions []string        `json:"transcriptions,omitempty"`
	AudioFiles     []WordAudioFile `json:"audioFiles,omitempty"`
	DefPairs       []DefPair       `json:"defPairs"`
}

type WordAudioFile struct {
	Url           string  `json:"url"`
	Accent        *string `json:"accent,omitempty"`
	Transcription *string `json:"transcription,omitempty"`
	Text          *string `json:"text,omitempty"`
}

type DefPair struct {
	PartOfSpeech api.PartOfSpeech `json:"partOfSpeech,omitempty"`
	DefEntries   []DefEntry       `json:"defEntries,omitempty"`
}

type DefEntry struct {
	Definition Definition `json:"definition"`
	Examples   []string   `json:"examples,omitempty"`
	Synonyms   []string   `json:"synonyms,omitempty"`
	Antonyms   []string   `json:"antonyms,omitempty"`
}

type Definition struct {
	Value  string   `json:"value"`
	Labels []string `json:"labels,omitempty"`
}
