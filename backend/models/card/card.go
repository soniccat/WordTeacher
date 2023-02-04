package card

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/partofspeech"
	"models/tools"
)

type CardProgress struct {
	CurrentLevel     int
	LastMistakeCount int
	LastLessonDate   string
}

type ApiCard struct {
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
	Progress            *CardProgress             `json:"progress"`
	CreationDate        string                    `json:"creationDate"`
	ModificationDate    string                    `json:"modificationDate"`
}

func (c *ApiCard) IsEqual(a *ApiCard) bool {
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

	return true
}

func (c *ApiCard) WithoutIds() *ApiCard {
	return &ApiCard{
		Id:                  "",
		Term:                c.Term,
		Transcription:       c.Transcription,
		PartOfSpeech:        c.PartOfSpeech,
		Definitions:         c.Definitions,
		Synonyms:            c.Synonyms,
		Examples:            c.Examples,
		DefinitionTermSpans: c.DefinitionTermSpans,
		ExampleTermSpans:    c.ExampleTermSpans,
		UserId:              "",
		CreationId:          c.CreationId,
		Progress:            c.Progress,
		CreationDate:        c.CreationDate,
		ModificationDate:    c.ModificationDate,
	}
}

func (c *ApiCard) ToDb() (*DbCard, error) {
	cardDbId, err := tools.ParseObjectID(c.Id)
	if err != nil {
		return nil, err
	}

	cardDbUserId, err := tools.ParseObjectID(c.UserId)
	if err != nil {
		return nil, err
	}

	return &DbCard{
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
		Progress:            c.Progress,
		CreationDate:        c.CreationDate,
		ModificationDate:    c.ModificationDate,
	}, nil
}

type DbCard struct {
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
	Progress            *CardProgress             `bson:"progress"`
	CreationDate        string                    `bson:"creationDate"`
	ModificationDate    string                    `bson:"modificationDate"`
}

func (c *DbCard) IsEqual(a *DbCard) bool {
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

	return true
}

type Span struct {
	Start int
	End   int
}

func (c *DbCard) ToApi() *ApiCard {
	return &ApiCard{
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
		Progress:            c.Progress,
		CreationDate:        c.CreationDate,
		ModificationDate:    c.ModificationDate,
	}
}
