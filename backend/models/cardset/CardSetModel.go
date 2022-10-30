package cardset

import (
	"context"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"models/logger"
)

type CardSetModel struct {
	logger            *logger.Logger
	cardSetCollection *mongo.Collection
	cardCollection    *mongo.Collection
}

func (m *CardSetModel) InsertCardSet(context context.Context, cardSet *CardSet, userId *primitive.ObjectID) (*CardSet, error) {

	userCounter, err := m.GetNewUserCounter(context)
	if err != nil {
		return nil, err
	}

	user.Counter = userCounter
	res, err := m.UserCollection.InsertOne(context, user)
	if err != nil {
		return nil, err
	}

	objId, ok := res.InsertedID.(primitive.ObjectID)
	if !ok {
		return nil, errors.New("InsertUser, InsertedID cast")
	}

	var newUser = *user
	newUser.ID = objId

	return &newUser, nil
}
