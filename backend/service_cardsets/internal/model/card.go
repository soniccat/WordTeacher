package model

import (
	"api"
	"context"
	"tools"

	"go.mongodb.org/mongo-driver/bson/primitive"

	cardsetsgrpc "service_cardsets/pkg/grpc/service_cardsets/api"
)

func ApiCardToDb(ctx context.Context, c *api.Card) (*DbCard, error) {
	cardDbId, err := tools.ParseObjectIDOrEmpty(ctx, c.Id)
	if err != nil {
		return nil, err
	}

	cardDbUserId, err := tools.ParseObjectID(ctx, c.UserId)
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

func (c *DbCard) ToApi() *api.Card {
	apiCard := &api.Card{
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
	if c.Id != nil {
		apiCard.Id = c.Id.Hex()
	} else {
		apiCard.Id = ""
	}

	return apiCard
}

func (c *DbCard) ToGrpc() *cardsetsgrpc.Card {
	return &cardsetsgrpc.Card{
		Id:               c.Id.Hex(),
		Term:             c.Term,
		Transcription:    c.Transcription,
		PartOfSpeech:     cardsetsgrpc.PartOfSpeech(int32(c.PartOfSpeech)),
		Definitions:      c.Definitions,
		Synonyms:         c.Synonyms,
		Examples:         c.Examples,
		UserId:           c.UserId.Hex(),
		CreationDate:     c.CreationDate,
		ModificationDate: c.ModificationDate,
	}
}
