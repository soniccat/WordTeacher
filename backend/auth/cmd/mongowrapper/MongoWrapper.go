package mongowrapper

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
	Client     *mongo.Client
	Context    *context.Context
	cancelFunc *context.CancelFunc
}

func New(mongoURI *string, enableCredentials *bool) (*MongoWrapper, error) {
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
		Client:     client,
		Context:    &ctx,
		cancelFunc: &cancel,
	}, nil
}

func (mw *MongoWrapper) Connect() error {
	return mw.Client.Connect(*mw.Context)
}

func (mw *MongoWrapper) Stop() {
	(*mw.cancelFunc)()
	if err := mw.Client.Disconnect(*mw.Context); err != nil {
		panic(err)
	}
}
