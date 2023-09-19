package wiktionary

import (
	"context"
	"log"
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

type Contract interface {
	Definitions(ctx context.Context, term string) ([]Word, error)
	CreateIndexIfNeeded(ctx context.Context) error
}

type Repository struct {
	Logger         *logger.Logger
	MongoClient    *mongo.Client
	WordCollection *mongo.Collection
}

func New(logger *logger.Logger, mongoClient *mongo.Client) Contract {
	model := &Repository{
		Logger:         logger,
		MongoClient:    mongoClient,
		WordCollection: mongoClient.Database(mongowrapper.MongoDatabaseWiktionary).Collection(mongowrapper.MongoCollectionWiktionaryWords),
	}

	return model
}

func (r *Repository) Definitions(ctx context.Context, term string) ([]Word, error) {
	cursor, err := r.WordCollection.Find(ctx, bson.M{"term": term}, options.Find().SetSort(bson.M{"order": 1}))

	if err != nil {
		return nil, err
	}

	var result []Word
	err = cursor.All(ctx, &result)
	if err != nil {
		return nil, err
	}

	return result, err
}

func (m *Repository) CreateIndexIfNeeded(ctx context.Context) error {
	cursor, err := m.WordCollection.Indexes().List(ctx)

	if err != nil {
		log.Fatal(err)
	}

	var result []bson.M
	if err = cursor.All(ctx, &result); err != nil {
		return err
	}

	var termIndexName = "term_index"
	for i := range result {
		if name, ok := result[i]["name"]; ok {
			if name == termIndexName {
				return nil
			}
		}
	}

	indexModel := mongo.IndexModel{
		Keys: bson.D{
			{Key: "term", Value: -1},
		},
		Options: &options.IndexOptions{
			Name: &termIndexName,
		},
	}
	_, err = m.WordCollection.Indexes().CreateOne(ctx, indexModel)
	if err != nil {
		return err
	}

	return nil
}
