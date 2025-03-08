package mongowrapper

import (
	"tools"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

type MongoIdWrapper struct {
	Id primitive.ObjectID `bson:"_id"`
}

type MongoIdWrapperList []MongoIdWrapper

func (l *MongoIdWrapperList) ToMongoIds() []primitive.ObjectID {
	return tools.Map[MongoIdWrapper](*l, func(t MongoIdWrapper) primitive.ObjectID {
		return t.Id
	})
}

type MongoStringIdWrapper struct {
	Id string `bson:"_id"`
}

type MongoStringIdWrapperList []MongoStringIdWrapper
