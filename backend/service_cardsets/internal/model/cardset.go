package model

import (
	"api"
	"context"
	"tools"

	"go.mongodb.org/mongo-driver/bson/primitive"

	cardsetsgrpc "service_cardsets/pkg/grpc/service_cardsets/api"
)

func ApiCardSetToDb(ctx context.Context, cs *api.CardSet) (*DbCardSet, error) {
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

	cardSetDbs, err := tools.MapOrError(cs.Cards, func(c *api.Card) (*DbCard, error) {
		return ApiCardToDb(ctx, c)
	})
	if err != nil {
		return nil, err
	}

	cardSetDb := &DbCardSet{
		Id:               id,
		Name:             cs.Name,
		Cards:            cardSetDbs,
		UserId:           userId,
		CreationDate:     creationDate,
		ModificationDate: modificationDateTime,
		CreationId:       cs.CreationId,
		Info: DbCardSetInfo{
			Description: cs.Info.Description,
			Source:      cs.Info.Source,
		},
		IsAvailableInSearch: cs.IsAvailableInSearch,
	}
	return cardSetDb, nil
}

type DbCardSet struct {
	Id                  *primitive.ObjectID `bson:"_id,omitempty"`
	Name                string              `bson:"name"`
	Tags                []string            `bson:"tags"`
	Cards               []*DbCard           `bson:"cards"`
	UserId              *primitive.ObjectID `bson:"userId"`
	CreationDate        primitive.DateTime  `bson:"creationDate"`
	ModificationDate    primitive.DateTime  `bson:"modificationDate"`
	CreationId          string              `bson:"creationId"`
	IsDeleted           bool                `bson:"isDeleted"`
	Info                DbCardSetInfo       `bson:"info"`
	IsAvailableInSearch bool                `bson:"isAvailableInSearch"`
}

type DbCardSetInfo struct {
	Description string
	Source      *string // url
}

func (cs *DbCardSet) ToApi() *api.CardSet {
	return &api.CardSet{
		Id:               cs.Id.Hex(),
		Name:             cs.Name,
		Tags:             cs.Tags,
		Cards:            tools.Map(cs.Cards, func(cardDb *DbCard) *api.Card { return cardDb.ToApi() }),
		UserId:           cs.UserId.Hex(),
		CreationDate:     tools.DbDateToApiDate(cs.CreationDate),
		ModificationDate: tools.DbDateToApiDate(cs.ModificationDate),
		CreationId:       cs.CreationId,
		Info: api.CardSetInfo{
			Description: cs.Info.Description,
			Source:      cs.Info.Source,
		},
		IsAvailableInSearch: cs.IsAvailableInSearch,
	}
}

func (cs *DbCardSet) ToGrpc() *cardsetsgrpc.CardSet {
	return &cardsetsgrpc.CardSet{
		Id:               cs.Id.Hex(),
		Name:             cs.Name,
		Tags:             cs.Tags,
		Cards:            tools.Map(cs.Cards, func(cardDb *DbCard) *cardsetsgrpc.Card { return cardDb.ToGrpc() }),
		UserId:           cs.UserId.Hex(),
		CreationDate:     tools.DbDateToApiDate(cs.CreationDate),
		ModificationDate: tools.DbDateToApiDate(cs.ModificationDate),
		Info: &cardsetsgrpc.CardSetInfo{
			Description: cs.Info.Description,
			Source:      cs.Info.Source,
		},
		IsAvailableInSearch: cs.IsAvailableInSearch,
	}
}

func DbCardSetsToApi(cs []*DbCardSet) []*api.CardSet {
	return tools.Map(cs, func(cardSetDb *DbCardSet) *api.CardSet {
		return cardSetDb.ToApi()
	})
}

func DbCardSetsToGrpc(cs []*DbCardSet) []*cardsetsgrpc.CardSet {
	return tools.Map(cs, func(cardSetDb *DbCardSet) *cardsetsgrpc.CardSet {
		return cardSetDb.ToGrpc()
	})
}
