package main

import (
	"context"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"os"
	"time"
)

const EnvMongoUsername = "MONGODB_USERNAME"
const EnvMongoPassword = "MONGODB_PASSWORD"

const MongoTimeout = 20 * time.Second

type MongoWrapper struct {
	client     *mongo.Client
	context    *context.Context
	cancelFunc *context.CancelFunc
}

func createMongoWrapper(mongoURI *string, enableCredentials *bool) (*MongoWrapper, error) {
	co := options.Client().ApplyURI(*mongoURI)
	if *enableCredentials {
		co.Auth = &options.Credential{
			Username: os.Getenv(EnvMongoUsername),
			Password: os.Getenv(EnvMongoPassword),
		}
	}

	client, err := mongo.NewClient(co)
	if err != nil {
		return nil, err
	}

	ctx, cancel := context.WithTimeout(context.Background(), MongoTimeout)

	return &MongoWrapper{
		client:     client,
		context:    &ctx,
		cancelFunc: &cancel,
	}, nil
}

func (mw *MongoWrapper) connect() error {
	return mw.client.Connect(*mw.context)
}

func (mw *MongoWrapper) stop() {
	(*mw.cancelFunc)()
	if err := mw.client.Disconnect(*mw.context); err != nil {
		panic(err)
	}
}
