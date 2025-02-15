package model

import "go.mongodb.org/mongo-driver/bson/primitive"

type DbHeadline struct {
	Id          *primitive.ObjectID `bson:"_id,omitempty"`
	Title       string              `bson:"title"`
	Description *string             `bson:"description,omitempty"`
	Link        string              `bson:"link"`
}
