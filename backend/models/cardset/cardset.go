package cardset

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/card"
	"models/tools"
	"time"
)

type CardSetApi struct {
	Id               string          `json:"id,omitempty"`
	Name             string          `json:"name"`
	Cards            []*card.CardApi `json:"cards"`
	UserId           string          `json:"userId"` // TODO: consider several owners via a permission filed
	CreationDate     string          `json:"creationDate"`
	ModificationDate *string         `json:"modificationDate,omitempty"`
	CreationId       string          `json:"creationId"`
}

func (cs *CardSetApi) IsEqual(a *CardSetApi) bool {
	if cs.Id != a.Id {
		return false
	}
	if cs.Name != a.Name {
		return false
	}
	if cs.UserId != a.UserId {
		return false
	}
	if cs.CreationDate != a.CreationDate {
		return false
	}
	if cs.ModificationDate != a.ModificationDate {
		return false
	}
	if cs.CreationId != a.CreationId {
		return false
	}
	if len(cs.Cards) != len(a.Cards) {
		return false
	}
	for i, c := range cs.Cards {
		if !c.IsEqual(a.Cards[i]) {
			return false
		}
	}

	return true
}

func (cs *CardSetApi) toDb() (*CardSetDb, error) {
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

	modificationDateTime, err := tools.ApiDatePtrToDbDatePtr(cs.ModificationDate)
	if err != nil {
		return nil, err
	}

	cardSetDbs, err := tools.MapOrError(cs.Cards, func(card *card.CardApi) (*card.CardDb, error) {
		return card.ToDb()
	})
	if err != nil {
		return nil, err
	}

	cardSetDb := &CardSetDb{
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

type CardSetDb struct {
	Id               *primitive.ObjectID `bson:"_id,omitempty"`
	Name             string              `bson:"name"`
	Cards            []*card.CardDb      `bson:"cards"`
	UserId           *primitive.ObjectID `bson:"userId"`
	CreationDate     primitive.DateTime  `bson:"creationDate"`
	ModificationDate *primitive.DateTime `bson:"modificationDate,omitempty"`
	CreationId       string              `bson:"creationId"`
}

func (cs *CardSetDb) ToApi() *CardSetApi {
	var md *string
	if cs.ModificationDate != nil {
		md = tools.Ptr(cs.ModificationDate.Time().UTC().Format(time.RFC3339))
	}

	return &CardSetApi{
		Id:               cs.Id.Hex(),
		Name:             cs.Name,
		Cards:            tools.Map(cs.Cards, func(cardDb *card.CardDb) *card.CardApi { return cardDb.ToApi() }),
		UserId:           cs.UserId.Hex(),
		CreationDate:     cs.CreationDate.Time().UTC().Format(time.RFC3339),
		ModificationDate: md,
		CreationId:       cs.CreationId,
	}
}
