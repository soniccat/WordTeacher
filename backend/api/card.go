package api

import (
	"tools"
)

type CardProgress struct {
	CurrentLevel     int    `json:"currentLevel" bson:"_id,omitempty"`
	LastMistakeCount int    `json:"lastMistakeCount" bson:"lastMistakeCount"`
	LastLessonDate   string `json:"lastLessonDate,omitempty" bson:"lastLessonDate,omitempty"`
}

type Span struct {
	Start int `json:"start" bson:"start"`
	End   int `json:"end" bson:"end"`
}

type Card struct {
	Id                          string        `json:"id"`
	Term                        string        `json:"term"`
	Transcription               *string       `json:"transcription,omitempty"`
	PartOfSpeech                PartOfSpeech  `json:"partOfSpeech"`
	Definitions                 []string      `json:"definitions"`
	Synonyms                    []string      `json:"synonyms"`
	Examples                    []string      `json:"examples"`
	DefinitionTermSpans         [][]Span      `json:"definitionTermSpans"`
	ExampleTermSpans            [][]Span      `json:"exampleTermSpans"`
	UserId                      string        `json:"userId"`
	CreationId                  string        `json:"creationId"`
	Progress                    *CardProgress `json:"progress"`
	CreationDate                string        `json:"creationDate"`
	ModificationDate            string        `json:"modificationDate"`
	NeedToUpdateDefinitionSpans bool          `json:"needToUpdateDefinitionSpans"`
	NeedToUpdateExampleSpans    bool          `json:"needToUpdateExampleSpans"`
}

func (c *Card) IsEqual(a *Card) bool {
	if c.Id != a.Id {
		return false
	}
	if c.Term != a.Term {
		return false
	}
	if c.Transcription != a.Transcription {
		return false
	}
	if c.PartOfSpeech != a.PartOfSpeech {
		return false
	}
	if !tools.SliceComparableEqual(c.Definitions, a.Definitions) {
		return false
	}
	if !tools.SliceComparableEqual(c.Synonyms, a.Synonyms) {
		return false
	}
	if !tools.SliceComparableEqual(c.Examples, a.Examples) {
		return false
	}
	if !tools.DoubleSliceComparableEqual(c.DefinitionTermSpans, a.DefinitionTermSpans) {
		return false
	}
	if !tools.DoubleSliceComparableEqual(c.ExampleTermSpans, a.ExampleTermSpans) {
		return false
	}
	if c.UserId != a.UserId {
		return false
	}
	if c.CreationId != a.CreationId {
		return false
	}
	if !tools.ComparePtrs(c.Progress, a.Progress) {
		return false
	}
	if c.CreationDate != a.CreationDate {
		return false
	}
	if c.ModificationDate != a.ModificationDate {
		return false
	}
	if c.NeedToUpdateDefinitionSpans != a.NeedToUpdateDefinitionSpans {
		return false
	}
	if c.NeedToUpdateExampleSpans != a.NeedToUpdateExampleSpans {
		return false
	}

	return true
}

func (c *Card) WithoutIds() *Card {
	return &Card{
		Id:                          "",
		Term:                        c.Term,
		Transcription:               c.Transcription,
		PartOfSpeech:                c.PartOfSpeech,
		Definitions:                 c.Definitions,
		Synonyms:                    c.Synonyms,
		Examples:                    c.Examples,
		DefinitionTermSpans:         c.DefinitionTermSpans,
		ExampleTermSpans:            c.ExampleTermSpans,
		UserId:                      "",
		CreationId:                  c.CreationId,
		Progress:                    c.Progress,
		CreationDate:                c.CreationDate,
		ModificationDate:            c.ModificationDate,
		NeedToUpdateDefinitionSpans: c.NeedToUpdateDefinitionSpans,
		NeedToUpdateExampleSpans:    c.NeedToUpdateExampleSpans,
	}
}
