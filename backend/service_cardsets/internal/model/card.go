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

	creationDate, err := tools.ApiDateToDbDate(ctx, c.CreationDate)
	if err != nil {
		return nil, err
	}

	modificationDate, err := tools.ApiDateToDbDate(ctx, c.ModificationDate)
	if err != nil {
		return nil, err
	}

	return &DbCard{
		Id:                          cardDbId,
		Term:                        c.Term,
		Transcriptions:              c.ResultTranscriptions(),
		AudioFiles:                  c.AudioFiles,
		PartOfSpeech:                c.PartOfSpeech,
		Definitions:                 c.Definitions,
		Labels:                      c.Labels,
		Synonyms:                    c.Synonyms,
		Examples:                    c.Examples,
		DefinitionTermSpans:         c.DefinitionTermSpans,
		ExampleTermSpans:            c.ExampleTermSpans,
		UserId:                      cardDbUserId,
		CreationId:                  c.CreationId,
		Progress:                    c.Progress,
		CreationDate:                creationDate,
		ModificationDate:            modificationDate,
		NeedToUpdateDefinitionSpans: c.NeedToUpdateDefinitionSpans,
		NeedToUpdateExampleSpans:    c.NeedToUpdateExampleSpans,
	}, nil
}

// TODO: remove, just use api.Card instead
type DbCard struct {
	Id   *primitive.ObjectID `bson:"_id,omitempty"`
	Term string              `bson:"term"`
	// Deprecated: use Transcriptions instead
	Transcription               *string             `bson:"transcription,omitempty"`
	Transcriptions              []string            `bson:"transcriptions,omitempty"`
	AudioFiles                  []api.AudioFile     `json:"audioFiles,omitempty"`
	PartOfSpeech                api.PartOfSpeech    `bson:"partOfSpeech"`
	Definitions                 []string            `bson:"definitions"`
	Labels                      []string            `bson:"labels"`
	Synonyms                    []string            `bson:"synonyms"`
	Examples                    []string            `bson:"examples"`
	DefinitionTermSpans         [][]api.Span        `bson:"definitionTermSpans"`
	ExampleTermSpans            [][]api.Span        `bson:"exampleTermSpans"`
	UserId                      *primitive.ObjectID `bson:"userId"`
	CreationId                  string              `bson:"creationId"`
	Progress                    *api.CardProgress   `bson:"progress"`
	CreationDate                primitive.DateTime  `bson:"creationDate"`
	ModificationDate            primitive.DateTime  `bson:"modificationDate"`
	NeedToUpdateDefinitionSpans bool                `bson:"needToUpdateDefinitionSpans"`
	NeedToUpdateExampleSpans    bool                `bson:"needToUpdateExampleSpans"`
}

func (c *DbCard) ToApi() *api.Card {
	apiCard := &api.Card{
		Term:                        c.Term,
		Transcription:               c.ResultTranscription(),
		Transcriptions:              c.ResultTranscriptions(),
		AudioFiles:                  c.AudioFiles,
		PartOfSpeech:                c.PartOfSpeech,
		Definitions:                 c.Definitions,
		Labels:                      c.Labels,
		Synonyms:                    c.Synonyms,
		Examples:                    c.Examples,
		DefinitionTermSpans:         c.DefinitionTermSpans,
		ExampleTermSpans:            c.ExampleTermSpans,
		UserId:                      c.UserId.Hex(),
		CreationId:                  c.CreationId,
		Progress:                    c.Progress,
		CreationDate:                tools.DbDateToApiDate(c.CreationDate),
		ModificationDate:            tools.DbDateToApiDate(c.ModificationDate),
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
		PartOfSpeech:     cardsetsgrpc.PartOfSpeech(int32(c.PartOfSpeech)),
		Definitions:      c.Definitions,
		Labels:           c.Labels,
		Synonyms:         c.Synonyms,
		Examples:         c.Examples,
		UserId:           c.UserId.Hex(),
		CreationDate:     tools.DbDateToApiDate(c.CreationDate),
		ModificationDate: tools.DbDateToApiDate(c.ModificationDate),
		Transcriptions:   c.ResultTranscriptions(),
		AudioFiles: tools.Map(c.AudioFiles, func(a api.AudioFile) *cardsetsgrpc.AudioFile {
			return &cardsetsgrpc.AudioFile{
				Url:           a.Url,
				Accent:        a.Accent,
				Transcription: a.Transcription,
				Text:          a.Text,
			}
		}),
	}
}

func (c *DbCard) ResultTranscription() *string {
	if len(c.Transcriptions) != 0 {
		return &c.Transcriptions[0]
	}

	return c.Transcription
}

func (c *DbCard) ResultTranscriptions() []string {
	if len(c.Transcriptions) != 0 {
		return c.Transcriptions
	}

	if c.Transcription != nil {
		return []string{*c.Transcription}
	}

	return nil
}
