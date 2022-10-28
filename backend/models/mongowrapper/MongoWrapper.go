package mongowrapper

import (
	"context"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"models/logger"
	"os"
	"time"
)

const EnvMongoUsername = "MONGODB_USERNAME"
const EnvMongoPassword = "MONGODB_PASSWORD"

const MongoTimeout = 20 * time.Second

// collections
const (
	MongoDatabaseUsers         = "users"
	MongoCollectionUsers       = "users"
	MongoCollectionUserCounter = "counter"
	MongoCollectionAuthTokens  = "authTokens"
)

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

type MongoApp interface {
	GetLogger() *logger.Logger
	SetMongoWrapper(*MongoWrapper)
}

func SetupMongo(app MongoApp, mongoURI *string, enableCredentials *bool) error {
	mongoWrapper, err := New(mongoURI, enableCredentials)
	if err != nil {
		app.GetLogger().Error.Printf("createMongoWrapper failed: %s", err.Error())
		return err
	}

	if err = mongoWrapper.Connect(); err != nil {
		app.GetLogger().Error.Printf("mongoWrapper.connect() failed: %s", err.Error())
		return err
	}

	app.SetMongoWrapper(mongoWrapper)
	return nil
}
