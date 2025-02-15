package model

import "go.mongodb.org/mongo-driver/bson/primitive"

type DbHeadline struct {
	Id          *primitive.ObjectID `bson:"_id,omitempty"`
	Title       string              `bson:"title"`
	Description *string             `bson:"description,omitempty"`
	Link        string              `bson:"link"`
	PubDate     *primitive.DateTime `bson:"pubDate,omitempty"`
	UpdateDate  *primitive.DateTime `bson:"updateDate,omitempty"`
	Creator     *primitive.DateTime `bson:"creator,omitempty"`
}
