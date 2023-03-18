package cardset

import (
	"api"
	"service_cardsets/cmd/internal/card"
	"time"
	"tools"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

func ApiCardSetToDb(cs *api.ApiCardSet) (*DbCardSet, error) {
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

	cardSetDbs, err := tools.MapOrError(cs.Cards, func(c *api.ApiCard) (*card.DbCard, error) {
		return card.ApiCardToDb(c)
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
	Cards            []*card.DbCard      `bson:"cards"`
	UserId           *primitive.ObjectID `bson:"userId"`
	CreationDate     primitive.DateTime  `bson:"creationDate"`
	ModificationDate primitive.DateTime  `bson:"modificationDate"`
	CreationId       string              `bson:"creationId"`
}

func (cs *DbCardSet) ToApi() *api.ApiCardSet {
	return &api.ApiCardSet{
		Id:               cs.Id.Hex(),
		Name:             cs.Name,
		Cards:            tools.Map(cs.Cards, func(cardDb *card.DbCard) *api.ApiCard { return cardDb.ToApi() }),
		UserId:           cs.UserId.Hex(),
		CreationDate:     cs.CreationDate.Time().UTC().Format(time.RFC3339),
		ModificationDate: cs.ModificationDate.Time().UTC().Format(time.RFC3339),
		CreationId:       cs.CreationId,
	}
}

func DbCardSetsToApi(cs []*DbCardSet) []*api.ApiCardSet {
	return tools.Map(cs, func(cardSetDb *DbCardSet) *api.ApiCardSet {
		return cardSetDb.ToApi()
	})
}
