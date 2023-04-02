package cardsetsearch

import (
	"api"
	cardSetsRabbitmq "service_cardsets/pkg/rabbitmq"
	"time"
	"tools"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

func MessageCardSetToDb(cs *cardSetsRabbitmq.CardSet) (*DbCardSet, error) {
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

	terms := tools.Map(cs.Cards, func(c *cardSetsRabbitmq.Card) string {
		return c.Term
	})

	cardSetDb := &DbCardSet{
		CardSetId:        id,
		Name:             cs.Name,
		Description:      cs.Description,
		Tags:             cs.Tags,
		UserId:           userId,
		CreationDate:     creationDate,
		ModificationDate: modificationDateTime,
		Terms:            terms,
	}
	return cardSetDb, nil
}

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
