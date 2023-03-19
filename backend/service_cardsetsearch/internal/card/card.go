package card

import (
	"api"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"tools"
)

func ApiCardToDb(c *api.Card) (*DbCard, error) {
	cardDbId, err := tools.ParseObjectID(c.Id)
	if err != nil {
		return nil, err
	}

	cardDbUserId, err := tools.ParseObjectID(c.UserId)
	if err != nil {
		return nil, err
	}

	return &DbCard{
		Id:                          cardDbId,
		Term:                        c.Term,
		Transcription:               c.Transcription,
		PartOfSpeech:                c.PartOfSpeech,
		Definitions:                 c.Definitions,
		Synonyms:                    c.Synonyms,
		Examples:                    c.Examples,
		DefinitionTermSpans:         c.DefinitionTermSpans,
		ExampleTermSpans:            c.ExampleTermSpans,
		UserId:                      cardDbUserId,
		CreationId:                  c.CreationId,
		Progress:                    c.Progress,
		CreationDate:                c.CreationDate,
		ModificationDate:            c.ModificationDate,
		NeedToUpdateDefinitionSpans: c.NeedToUpdateDefinitionSpans,
		NeedToUpdateExampleSpans:    c.NeedToUpdateExampleSpans,
	}, nil
}

type DbCard struct {
	Id                          *primitive.ObjectID `bson:"_id,omitempty"`
	Term                        string              `bson:"term"`
	Transcription               *string             `bson:"transcription,omitempty"`
	PartOfSpeech                api.PartOfSpeech    `bson:"partOfSpeech"`
	Definitions                 []string            `bson:"definitions"`
	Synonyms                    []string            `bson:"synonyms"`
	Examples                    []string            `bson:"examples"`
	DefinitionTermSpans         [][]api.Span        `bson:"definitionTermSpans"`
	ExampleTermSpans            [][]api.Span        `bson:"exampleTermSpans"`
	UserId                      *primitive.ObjectID `bson:"userId"`
	CreationId                  string              `bson:"creationId"`
	Progress                    *api.CardProgress   `bson:"progress"`
	CreationDate                string              `bson:"creationDate"`
	ModificationDate            string              `bson:"modificationDate"`
	NeedToUpdateDefinitionSpans bool                `bson:"needToUpdateDefinitionSpans"`
	NeedToUpdateExampleSpans    bool                `bson:"needToUpdateExampleSpans"`
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
	if c.NeedToUpdateDefinitionSpans != a.NeedToUpdateDefinitionSpans {
		return false
	}
	if c.NeedToUpdateExampleSpans != a.NeedToUpdateExampleSpans {
		return false
	}

	return true
}

func (c *DbCard) ToApi() *api.Card {
	return &api.Card{
		Id:                          c.Id.Hex(),
		Term:                        c.Term,
		Transcription:               c.Transcription,
		PartOfSpeech:                c.PartOfSpeech,
		Definitions:                 c.Definitions,
		Synonyms:                    c.Synonyms,
		Examples:                    c.Examples,
		DefinitionTermSpans:         c.DefinitionTermSpans,
		ExampleTermSpans:            c.ExampleTermSpans,
		UserId:                      c.UserId.Hex(),
		CreationId:                  c.CreationId,
		Progress:                    c.Progress,
		CreationDate:                c.CreationDate,
		ModificationDate:            c.ModificationDate,
		NeedToUpdateDefinitionSpans: c.NeedToUpdateDefinitionSpans,
		NeedToUpdateExampleSpans:    c.NeedToUpdateExampleSpans,
	}
}
