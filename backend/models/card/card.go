package card

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/partofspeech"
)

type CardApi struct {
	Id                  string                    `json:"id"`
	Term                string                    `json:"term"`
	Transcription       *string                   `json:"transcription,omitempty"`
	PartOfSpeech        partofspeech.PartOfSpeech `json:"partOfSpeech"`
	Definitions         []string                  `json:"definitions"`
	Synonyms            []string                  `json:"synonyms"`
	Examples            []string                  `json:"examples"`
	DefinitionTermSpans [][]Span                  `json:"definitionTermSpans"`
	ExampleTermSpans    [][]Span                  `json:"exampleTermSpans"`
	UserId              string                    `json:"userId"`
	CreationDate        string                    `json:"creationDate"`
	ModificationDate    *string                   `json:"modificationDate,omitempty"`
}

type CardDb struct {
	ID                  primitive.ObjectID        `bson:"_id,omitempty"`
	Term                string                    `bson:"term"`
	Transcription       *string                   `bson:"transcription,omitempty"`
	PartOfSpeech        partofspeech.PartOfSpeech `bson:"partOfSpeech"`
	Definitions         []string                  `bson:"definitions"`
	Synonyms            []string                  `bson:"synonyms"`
	Examples            []string                  `bson:"examples"`
	DefinitionTermSpans [][]Span                  `bson:"definitionTermSpans"`
	ExampleTermSpans    [][]Span                  `bson:"exampleTermSpans"`
	UserId              primitive.ObjectID        `bson:"userId"`
	CreationDate        primitive.DateTime        `bson:"creationDate"`
	ModificationDate    *primitive.DateTime       `bson:"modificationDate"`
}

type Span struct {
	start int
	end   int
}
