package model

import (
	"api"
	"tools"

	"go.mongodb.org/mongo-driver/bson/primitive"

	cardsetsgrpc "service_cardsets/pkg/grpc/service_cardsets/api"
)

func ApiCardSetToDb(cs *api.CardSet) (*DbCardSet, error) {
	id, err := tools.ParseObjectID(cs.Id)
	if err != nil {
		return nil, err
	}

	userId, err := tools.ParseObjectID(cs.UserId)
	if err != nil {
		return nil, err
	}

	creationDate, err := tools.ApiDateToDbDate(cs.CreationDate)
	if err != nil {
		return nil, err
	}

	modificationDateTime, err := tools.ApiDateToDbDate(cs.ModificationDate)
	if err != nil {
		return nil, err
	}

	cardSetDbs, err := tools.MapOrError(cs.Cards, func(c *api.Card) (*DbCard, error) {
		return ApiCardToDb(c)
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
	}
	return cardSetDb, nil
}

type DbCardSet struct {
	Id               *primitive.ObjectID `bson:"_id,omitempty"`
	Name             string              `bson:"name"`
	Description      string              `bson:"description"`
	Tags             []string            `bson:"tags"`
	Cards            []*DbCard           `bson:"cards"`
	UserId           *primitive.ObjectID `bson:"userId"`
	CreationDate     primitive.DateTime  `bson:"creationDate"`
	ModificationDate primitive.DateTime  `bson:"modificationDate"`
	CreationId       string              `bson:"creationId"`
	IsDeleted        bool                `bson:"isDeleted"`
}

func (cs *DbCardSet) ToApi() *api.CardSet {
	return &api.CardSet{
		Id:               cs.Id.Hex(),
		Name:             cs.Name,
		Description:      cs.Description,
		Tags:             cs.Tags,
		Cards:            tools.Map(cs.Cards, func(cardDb *DbCard) *api.Card { return cardDb.ToApi() }),
		UserId:           cs.UserId.Hex(),
		CreationDate:     tools.DbDateToApiDate(cs.CreationDate),
		ModificationDate: tools.DbDateToApiDate(cs.ModificationDate),
		CreationId:       cs.CreationId,
	}
}

func (cs *DbCardSet) ToGrpc() *cardsetsgrpc.CardSet {
	return &cardsetsgrpc.CardSet{
		Id:               cs.Id.Hex(),
		Name:             cs.Name,
		Description:      cs.Description,
		Tags:             cs.Tags,
		Cards:            tools.Map(cs.Cards, func(cardDb *DbCard) *cardsetsgrpc.Card { return cardDb.ToGrpc() }),
		UserId:           cs.UserId.Hex(),
		CreationDate:     tools.DbDateToApiDate(cs.CreationDate),
		ModificationDate: tools.DbDateToApiDate(cs.ModificationDate),
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
