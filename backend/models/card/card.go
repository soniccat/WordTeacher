package card

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/partofspeech"
)

type BaseCard interface {
	getTerm() string
	getTranscription() *string
	getPartOfSpeech() partofspeech.PartOfSpeech
	getDefinitions() []string
	getSynonyms() []string
	getExamples() []string
	getDefinitionTermSpans() [][]struct {
		a int
		b int
	}
	getExampleTermSpans() [][]struct {
		a int
		b int
	}
}

type Card struct {
	Term                string                    `json:"term"`
	Transcription       *string                   `json:"transcription,omitempty"`
	PartOfSpeech        partofspeech.PartOfSpeech `json:"partOfSpeech"`
	Definitions         []string                  `json:"definitions"`
	Synonyms            []string                  `json:"synonyms"`
	Examples            []string                  `json:"examples"`
	DefinitionTermSpans [][]struct {
		a int
		b int
	} `json:"definitionTermSpans"`
	ExampleTermSpans [][]struct {
		a int
		b int
	} `json:"exampleTermSpans"`
}

func (bc *Card) getTerm() string {
	return bc.Term
}

type MongoCard struct {
	ID                  *primitive.ObjectID       `bson:"_id,omitempty"`
	Term                string                    `bson:"term"`
	Transcription       *string                   `bson:"transcription,omitempty"`
	PartOfSpeech        partofspeech.PartOfSpeech `bson:"partOfSpeech"`
	Definitions         []string                  `bson:"definitions"`
	Synonyms            []string                  `bson:"synonyms"`
	Examples            []string                  `bson:"examples"`
	DefinitionTermSpans [][]struct {
		a int
		b int
	} `bson:"definitionTermSpans"`
	ExampleTermSpans [][]struct {
		a int
		b int
	} `bson:"exampleTermSpans"`
}

func (bc *MongoCard) getTerm() string {
	return bc.Term
}
