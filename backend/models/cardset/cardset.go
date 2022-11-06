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
	CreationId       *string         `json:"creationId"`
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

type CardSetDb struct {
	ID               primitive.ObjectID   `bson:"_id,omitempty"`
	Name             string               `bson:"name"`
	Cards            []primitive.ObjectID `bson:"cards"`
	UserId           *primitive.ObjectID  `bson:"userId"`
	CreationDate     primitive.DateTime   `bson:"creationDate"`
	ModificationDate *primitive.DateTime  `bson:"modificationDate,omitempty"`
	CreationId       *string              `bson:"creationId"`
}

func (cs *CardSetDb) ToApi(cards []*card.CardApi) *CardSetApi {
	var md *string
	if cs.ModificationDate != nil {
		md = tools.Ptr(cs.ModificationDate.Time().Format(time.RFC3339))
	}

	return &CardSetApi{
		Id:               cs.ID.Hex(),
		Name:             cs.Name,
		Cards:            cards,
		UserId:           cs.UserId.Hex(),
		CreationDate:     cs.CreationDate.Time().Format(time.RFC3339),
		ModificationDate: md,
		CreationId:       cs.CreationId,
	}
}
