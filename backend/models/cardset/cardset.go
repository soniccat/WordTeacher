package cardset

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/card"
)

type CardSetApi struct {
	Id               string          `json:"id,omitempty"`
	Name             string          `json:"name"`
	Cards            []*card.CardApi `json:"cards"`
	UserId           string          `json:"userId"`
	CreationDate     string          `json:"creationDate"`
	ModificationDate *string         `json:"modificationDate,omitempty"`
}

type CardSetDb struct {
	ID               primitive.ObjectID   `bson:"_id,omitempty"`
	Name             string               `bson:"name"`
	Cards            []primitive.ObjectID `bson:"cards"`
	UserId           primitive.ObjectID   `bson:"userId"`
	CreationDate     primitive.DateTime   `bson:"creationDate"`
	ModificationDate *primitive.DateTime  `bson:"modificationDate,omitempty"`
}
