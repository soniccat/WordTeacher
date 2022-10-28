package cardset

import (
	"go.mongodb.org/mongo-driver/mongo"
	"models/logger"
)

type CardSetModel struct {
	logger            *logger.Logger
	counterCollection *mongo.Collection
	userCollection    *mongo.Collection
	authTokens        *mongo.Collection
}
