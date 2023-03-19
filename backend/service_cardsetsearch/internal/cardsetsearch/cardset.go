package cardset

import (
	"api"
	"time"
	"tools"

	"go.mongodb.org/mongo-driver/bson/primitive"
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

	if err != nil {
		return nil, err
	}

	cardSetDb := &DbCardSet{
		Id:               id,
		Name:             cs.Name,
		Description:      cs.Description,
		Tags:             cs.Tags,
		UserId:           userId,
		CreationDate:     creationDate,
		ModificationDate: modificationDateTime,
	}
	return cardSetDb, nil
}

type DbCardSet struct {
	Id               *primitive.ObjectID `bson:"_id,omitempty"`
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
		Id:               cs.Id.Hex(),
		Name:             cs.Name,
		Description:      cs.Description,
		Tags:             cs.Tags,
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
