package model

import (
	"api"
	"context"
	cardsetsgrpc "service_cardsets/pkg/grpc/service_cardsets/api"
	"slices"
	"time"
	"tools"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

type DbCardSet struct {
	Id               *primitive.ObjectID `bson:"_id,omitempty"`
	CardSetId        *primitive.ObjectID `bson:"cardSetId,omitempty"`
	Name             string              `bson:"name"`
	Tags             []string            `bson:"tags"`
	UserId           *primitive.ObjectID `bson:"userId"`
	CreationDate     primitive.DateTime  `bson:"creationDate"`
	ModificationDate primitive.DateTime  `bson:"modificationDate"`
	Terms            []string            `bson:"terms"`
	Info             DbCardSetInfo       `bson:"info"`
}

type DbCardSetInfo struct {
	Description string
	Source      *string // url
}

func (cs *DbCardSet) ToApi() *api.CardSet {
	return &api.CardSet{
		Id:               cs.CardSetId.Hex(),
		Name:             cs.Name,
		Tags:             cs.Tags,
		Terms:            cs.Terms,
		UserId:           cs.UserId.Hex(),
		CreationDate:     cs.CreationDate.Time().UTC().Format(time.RFC3339Nano),
		ModificationDate: cs.ModificationDate.Time().UTC().Format(time.RFC3339Nano),
		Info: api.CardSetInfo{
			Description: cs.Info.Description,
			Source:      cs.Info.Source,
		},
	}
}

func DbCardSetsToApi(cs []*DbCardSet) []*api.CardSet {
	return tools.Map(cs, func(cardSetDb *DbCardSet) *api.CardSet {
		return cardSetDb.ToApi()
	})
}

func GRPCCardSetToDb(ctx context.Context, cs *cardsetsgrpc.CardSet) (*DbCardSet, error) {
	id, err := tools.ParseObjectID(ctx, cs.Id)
	if err != nil {
		return nil, err
	}

	userId, err := tools.ParseObjectID(ctx, cs.UserId)
	if err != nil {
		return nil, err
	}

	creationDate, err := tools.ApiDateToDbDate(ctx, cs.CreationDate)
	if err != nil {
		return nil, err
	}

	modificationDateTime, err := tools.ApiDateToDbDate(ctx, cs.ModificationDate)
	if err != nil {
		return nil, err
	}

	terms := tools.Map(cs.Cards, func(c *cardsetsgrpc.Card) string {
		return c.Term
	})
	// sort to find distance between card sets faster
	slices.Sort(terms)

	cardSetDb := &DbCardSet{
		CardSetId:        id,
		Name:             cs.Name,
		Tags:             cs.Tags,
		UserId:           userId,
		CreationDate:     creationDate,
		ModificationDate: modificationDateTime,
		Terms:            terms,
		Info: DbCardSetInfo{
			Description: cs.Info.Description,
			Source:      cs.Info.Source,
		},
	}
	return cardSetDb, nil
}
