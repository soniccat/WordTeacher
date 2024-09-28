package repository

import (
	"context"
	"tools/logger"
	"tools/mongowrapper"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

type Word struct {
	Term           string                  `bson:"term,omitempty"`
	Transcriptions []string                `bson:"transcriptions,omitempty"`
	Etymology      int                     `bson:"etymology,omitempty"`
	CatDefs        map[string][]Definition `bson:"definitions,omitempty"`
}

type Definition struct {
	Def      string   `bson:"def,omitempty"`
	Examples []string `bson:"examples,omitempty"`
	Synonyms []string `bson:"synonyms,omitempty"`
	Antonyms []string `bson:"antonyms,omitempty"`
}

type Repository struct {
	Logger         *logger.Logger
	MongoWrapper   *mongowrapper.MongoWrapper
	WordCollection *mongo.Collection
}

func New(logger *logger.Logger, mongoWrapper *mongowrapper.MongoWrapper) Repository {
	model := Repository{
		Logger:         logger,
		MongoWrapper:   mongoWrapper,
		WordCollection: mongoWrapper.Client.Database(mongowrapper.MongoDatabaseWiktionary).Collection(mongowrapper.MongoCollectionWiktionaryWords),
	}

	return model
}

func (r *Repository) Definitions(ctx context.Context, term string) ([]Word, error) {
	cursor, err := r.WordCollection.Find(
		ctx,
		bson.M{"term": term},
		options.Find().SetSort(bson.M{"order": 1}),
	)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	var result []Word
	err = cursor.All(ctx, &result)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	return result, nil
}

func (m *Repository) CreateIndexIfNeeded() error {
	err := m.MongoWrapper.CreateIndexIfNeeded(m.WordCollection, "term")
	if err != nil {
		return logger.WrapError(m.MongoWrapper.Context, err)
	}

	return nil
}
