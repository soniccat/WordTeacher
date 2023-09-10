package model

import (
	"api"
	"time"
	"tools"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

type DbCardSet struct {
	Id               *primitive.ObjectID `bson:"_id,omitempty"`
	CardSetId        *primitive.ObjectID `bson:"cardSetId,omitempty"`
	Name             string              `bson:"name"`
	Description      string              `bson:"description"`
	Tags             []string            `bson:"tags"`
	UserId           *primitive.ObjectID `bson:"userId"`
	CreationDate     primitive.DateTime  `bson:"creationDate"`
	ModificationDate primitive.DateTime  `bson:"modificationDate"`
	Terms            []string            `bson:"terms"`
}

func (cs *DbCardSet) ToApi() *api.CardSet {
	return &api.CardSet{
		Id:               cs.CardSetId.Hex(),
		Name:             cs.Name,
		Description:      cs.Description,
		Tags:             cs.Tags,
		Terms:            cs.Terms,
		UserId:           cs.UserId.Hex(),
		CreationDate:     cs.CreationDate.Time().UTC().Format(time.RFC3339),
		ModificationDate: cs.ModificationDate.Time().UTC().Format(time.RFC3339),
	}
}

func DbCardSetsToApi(cs []*DbCardSet) []*api.CardSet {
	return tools.Map(cs, func(cardSetDb *DbCardSet) *api.CardSet {
		return cardSetDb.ToApi()
	})
}
