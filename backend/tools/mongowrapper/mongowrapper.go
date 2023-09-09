package mongowrapper

import (
	"context"
	"os"
	"time"
	"tools/logger"

	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

const EnvMongoUsername = "MONGODB_USERNAME"
const EnvMongoPassword = "MONGODB_PASSWORD"

const MongoTimeout = 20 * time.Second

// collections
const (
	MongoDatabaseUsers   = "users"
	MongoCollectionUsers = "users"

	MongoDatabaseCardSets     = "cardSets"
	MongoCollectionCardSets   = "cardSets"
	MongoCollectionAuthTokens = "authTokens"

	MongoDatabaseCardSetSearch   = "cardSetSearch"
	MongoCollectionCardSetSearch = "cardSets"
)

type MongoWrapper struct {
	Client     *mongo.Client
	Context    *context.Context
	cancelFunc *context.CancelFunc
}

func New(mongoURI string, enableCredentials bool) (*MongoWrapper, error) {
	co := options.Client().ApplyURI(mongoURI)
	if enableCredentials {
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

func (mw *MongoWrapper) Stop() error {
	if mw.cancelFunc != nil {
		(*mw.cancelFunc)()
		mw.cancelFunc = nil
	}

	if mw.Client != nil {
		if err := mw.Client.Disconnect(*mw.Context); err != nil {
			return err
		}
		mw.Client = nil
	}

	return nil
}

type MongoApp struct {
	MongoWrapper *MongoWrapper
}

func (m *MongoApp) SetupMongo(mongoURI string, enableCredentials bool, logger *logger.Logger) error {
	mongoWrapper, err := New(mongoURI, enableCredentials)
	if err != nil {
		logger.Error.Printf("createMongoWrapper failed: %s\n", err.Error())
		return err
	}

	if err = mongoWrapper.Connect(); err != nil {
		logger.Error.Printf("mongoWrapper.connect() failed: %s\n", err.Error())
		return err
	}

	m.MongoWrapper = mongoWrapper
	return nil
}
