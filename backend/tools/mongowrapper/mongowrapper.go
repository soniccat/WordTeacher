package mongowrapper

import (
	"context"
	"fmt"
	"os"
	"time"
	"tools/logger"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/writeconcern"
)

const EnvMongoUsername = "MONGODB_USERNAME"
const EnvMongoPassword = "MONGODB_PASSWORD"

const MongoTimeout = 20 * time.Second

// collections
const (
	// TODO: move that in proper services
	MongoDatabaseUsers   = "users"
	MongoCollectionUsers = "users"

	MongoDatabaseCardSets     = "cardSets"
	MongoCollectionCardSets   = "cardSets"
	MongoCollectionAuthTokens = "authTokens"

	MongoDatabaseCardSetSearch   = "cardSetSearch"
	MongoCollectionCardSetSearch = "cardSets"

	MongoDatabaseWiktionary                  = "wiktionary"
	MongoCollectionWiktionaryWords           = "words"
	MongoCollectionWiktionaryWordsV2         = "words2"
	MongoCollectionWiktionaryWordsV2Examples = "words2examples"
)

type MongoWrapper struct {
	Client     *mongo.Client
	Context    context.Context
	cancelFunc context.CancelFunc
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
		return nil, logger.WrapError(context.Background(), err)
	}

	ctx, cancel := context.WithTimeout(context.Background(), MongoTimeout)

	return &MongoWrapper{
		Client:     client,
		Context:    ctx,
		cancelFunc: cancel,
	}, nil
}

func (mw *MongoWrapper) Connect() error {
	return mw.Client.Connect(mw.Context)
}

func (mw *MongoWrapper) Stop() error {
	if mw.cancelFunc != nil {
		(mw.cancelFunc)()
		mw.cancelFunc = nil
	}

	if mw.Client != nil {
		if err := mw.Client.Disconnect(mw.Context); err != nil {
			return logger.WrapError(context.Background(), err)
		}
		mw.Client = nil
	}

	return nil
}

func (mw *MongoWrapper) CreateIndexIfNeeded(
	collection *mongo.Collection,
	fieldName string,
) error {
	cursor, err := collection.Indexes().List(mw.Context)
	if err != nil {
		return logger.WrapError(mw.Context, err)
	}

	var result []bson.M
	if err = cursor.All(mw.Context, &result); err != nil {
		return logger.WrapError(mw.Context, err)
	}

	var indexName = fieldName + "_index"
	for i := range result {
		if name, ok := result[i]["name"]; ok {
			if name == indexName {
				return nil
			}
		}
	}

	indexModel := mongo.IndexModel{
		Keys: bson.D{
			{Key: fieldName, Value: -1},
		},
		Options: &options.IndexOptions{
			Name: &indexName,
		},
	}
	_, err = collection.Indexes().CreateOne(mw.Context, indexModel)
	if err != nil {
		return logger.WrapError(mw.Context, err)
	}

	return nil
}

// TODO: remove MongoEnv and just use MongoWrapper
type MongoEnv struct {
	MongoWrapper *MongoWrapper
	Logger       *logger.Logger
}

func NewMongoEnv(logger *logger.Logger) MongoEnv {
	return MongoEnv{
		Logger: logger,
	}
}

func (m *MongoEnv) SetupMongo(mongoURI string, enableCredentials bool) error {
	mongoWrapper, err := New(mongoURI, enableCredentials)
	if err != nil {
		return logger.WrapError(context.Background(), fmt.Errorf("createMongoWrapper failed: %w", err))
	}

	if err = mongoWrapper.Connect(); err != nil {
		return logger.WrapError(context.Background(), fmt.Errorf("mongoWrapper.connect() failed: %w", err))
	}

	m.MongoWrapper = mongoWrapper
	return nil
}

func (m *MongoEnv) StopMongo() error {
	if m.MongoWrapper != nil {
		err := m.MongoWrapper.Stop()
		if err != nil {
			return logger.WrapError(context.Background(), fmt.Errorf("mongoWrapper.Stop() failed: %w", err))
		}
	}

	return nil
}

func (m *MongoEnv) MongoClient() *mongo.Client {
	return m.MongoWrapper.Client
}

func (m *MongoEnv) Collection(
	database string,
	collection string,
) *mongo.Collection {
	return m.MongoClient().Database(database).Collection(collection)
}

func (m *MongoEnv) StartTransaction(ctx context.Context, block func(tCtx context.Context) (interface{}, error)) (interface{}, error) {
	session, sessionErr := m.MongoClient().StartSession()
	if sessionErr != nil {
		return nil, sessionErr
	}

	defer func() {
		session.EndSession(ctx)
	}()

	wc := writeconcern.New(writeconcern.WMajority())
	txnOpts := options.Transaction().SetWriteConcern(wc)
	return session.WithTransaction(
		ctx,
		func(sCtx mongo.SessionContext) (interface{}, error) {
			response, sErr := block(sCtx)
			if sErr != nil {
				return nil, sErr
			}

			return response, nil
		},
		txnOpts,
	)
}
