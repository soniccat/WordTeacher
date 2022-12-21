package card

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/partofspeech"
	"models/tools"
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
	CreationId          string                    `json:"creationId"`
}

func (cs *CardApi) IsEqual(a *CardApi) bool {
	if cs.Id != a.Id {
		return false
	}
	if cs.Term != a.Term {
		return false
	}
	if cs.Transcription != a.Transcription {
		return false
	}
	if cs.PartOfSpeech != a.PartOfSpeech {
		return false
	}
	if !tools.SliceComparableEqual(cs.Definitions, a.Definitions) {
		return false
	}
	if !tools.SliceComparableEqual(cs.Synonyms, a.Synonyms) {
		return false
	}
	if !tools.SliceComparableEqual(cs.Examples, a.Examples) {
		return false
	}
	if !tools.DoubleSliceComparableEqual(cs.DefinitionTermSpans, a.DefinitionTermSpans) {
		return false
	}
	if !tools.DoubleSliceComparableEqual(cs.ExampleTermSpans, a.ExampleTermSpans) {
		return false
	}
	if cs.UserId != a.UserId {
		return false
	}
	if cs.CreationId != a.CreationId {
		return false
	}

	return true
}

func (c *CardApi) ToDb() (*CardDb, error) {
	cardDbId, err := tools.ParseObjectID(c.Id)
	if err != nil {
		return nil, err
	}

	cardDbUserId, err := tools.ParseObjectID(c.UserId)
	if err != nil {
		return nil, err
	}

	return &CardDb{
		Id:                  cardDbId,
		Term:                c.Term,
		Transcription:       c.Transcription,
		PartOfSpeech:        c.PartOfSpeech,
		Definitions:         c.Definitions,
		Synonyms:            c.Synonyms,
		Examples:            c.Examples,
		DefinitionTermSpans: c.DefinitionTermSpans,
		ExampleTermSpans:    c.ExampleTermSpans,
		UserId:              cardDbUserId,
		CreationId:          c.CreationId,
	}, nil
}

type CardDb struct {
	Id                  *primitive.ObjectID       `bson:"_id,omitempty"`
	Term                string                    `bson:"term"`
	Transcription       *string                   `bson:"transcription,omitempty"`
	PartOfSpeech        partofspeech.PartOfSpeech `bson:"partOfSpeech"`
	Definitions         []string                  `bson:"definitions"`
	Synonyms            []string                  `bson:"synonyms"`
	Examples            []string                  `bson:"examples"`
	DefinitionTermSpans [][]Span                  `bson:"definitionTermSpans"`
	ExampleTermSpans    [][]Span                  `bson:"exampleTermSpans"`
	UserId              *primitive.ObjectID       `bson:"userId"`
	CreationId          string                    `bson:"creationId"`
}

func (cs *CardDb) IsEqual(a *CardDb) bool {
	if cs.Id != a.Id {
		return false
	}
	if cs.Term != a.Term {
		return false
	}
	if cs.Transcription != a.Transcription {
		return false
	}
	if cs.PartOfSpeech != a.PartOfSpeech {
		return false
	}
	if !tools.SliceComparableEqual(cs.Definitions, a.Definitions) {
		return false
	}
	if !tools.SliceComparableEqual(cs.Synonyms, a.Synonyms) {
		return false
	}
	if !tools.SliceComparableEqual(cs.Examples, a.Examples) {
		return false
	}
	if !tools.DoubleSliceComparableEqual(cs.DefinitionTermSpans, a.DefinitionTermSpans) {
		return false
	}
	if !tools.DoubleSliceComparableEqual(cs.ExampleTermSpans, a.ExampleTermSpans) {
		return false
	}
	if cs.UserId != a.UserId {
		return false
	}
	if cs.CreationId != a.CreationId {
		return false
	}

	return true
}

type Span struct {
	Start int
	End   int
}

func (c *CardDb) ToApi() *CardApi {
	return &CardApi{
		Id:                  c.Id.Hex(),
		Term:                c.Term,
		Transcription:       c.Transcription,
		PartOfSpeech:        c.PartOfSpeech,
		Definitions:         c.Definitions,
		Synonyms:            c.Synonyms,
		Examples:            c.Examples,
		DefinitionTermSpans: c.DefinitionTermSpans,
		ExampleTermSpans:    c.ExampleTermSpans,
		UserId:              c.UserId.Hex(),
		CreationId:          c.CreationId,
	}
}
