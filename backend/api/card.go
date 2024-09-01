package api

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
	Labels                      []string      `json:"labels"`
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

func (c *Card) WithoutIds() *Card {
	return &Card{
		Id:                          "",
		Term:                        c.Term,
		Transcription:               c.Transcription,
		PartOfSpeech:                c.PartOfSpeech,
		Definitions:                 c.Definitions,
		Labels:                      c.Labels,
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
