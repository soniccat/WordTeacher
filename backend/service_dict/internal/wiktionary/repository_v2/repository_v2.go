package repository_v2

import (
	"context"
	"tools/logger"
	"tools/mongowrapper"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

type WordEntry struct {
	// Order          int           //`bson:"order"`
	Term           string        `bson:"term,omitempty"`
	Transcriptions []string      `bson:"transcriptions,omitempty"`
	Etymology      int           `bson:"etymology,omitempty"`
	DefPairs       []WordDefPair `bson:"defs,omitempty"`
}

type WordDefPair struct {
	PartOfSpeech string         `bson:"partofspeech,omitempty"`
	DefEntries   []WordDefEntry `bson:"defentries,omitempty"`
}

type WordDefEntry struct {
	Def      WordDef  `bson:"def,omitempty"`
	Examples []string `bson:"examples,omitempty"`
	Synonyms []string `bson:"synonyms,omitempty"`
	Antonyms []string `bson:"antonyms,omitempty"`
}

type WordDef struct {
	Value  string   `bson:"value,omitempty"`
	Labels []string `bson:"labels,omitempty"`
}

type WordExamples struct {
	Examples []WordExample `bson:"examples,omitempty"`
	Word     WordEntry     `bson:"word"`
}

type WordExample struct {
	Example       string  `bson:"example"`
	DefPairIndex  int     `bson:"defPairIndex"`
	DefEntryIndex int     `bson:"defEntryIndex"`
	ExampleIndex  int     `bson:"exampleIndex"`
	TextScore     float64 `bson:"textScore"`
}

type Repository struct {
	Logger                *logger.Logger
	MongoClient           *mongo.Client
	WordCollection        *mongo.Collection
	WordExampleCollection *mongo.Collection
}

func New(logger *logger.Logger, mongoClient *mongo.Client) Repository {
	model := Repository{
		Logger:                logger,
		MongoClient:           mongoClient,
		WordCollection:        mongoClient.Database(mongowrapper.MongoDatabaseWiktionary).Collection(mongowrapper.MongoCollectionWiktionaryWordsV2),
		WordExampleCollection: mongoClient.Database(mongowrapper.MongoDatabaseWiktionary).Collection(mongowrapper.MongoCollectionWiktionaryWordsV2Examples),
	}

	return model
}

func (r *Repository) Definitions(ctx context.Context, term string) ([]WordEntry, error) {
	cursor, err := r.WordCollection.Find(
		ctx,
		bson.M{"term": term},
		options.Find().SetSort(bson.M{"etymology": 1}),
	)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	var result []WordEntry
	err = cursor.All(ctx, &result)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	return result, nil
}

func (m *Repository) CreateIndexIfNeeded(ctx context.Context) error {
	cursor, err := m.WordCollection.Indexes().List(ctx)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	var result []bson.M
	if err = cursor.All(ctx, &result); err != nil {
		return logger.WrapError(ctx, err)
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
		return logger.WrapError(ctx, err)
	}

	return nil
}

func (r *Repository) WordExamples(ctx context.Context, text string, limit int) ([]WordExamples, error) {
	cursor, err := r.WordExampleCollection.Aggregate(ctx, bson.A{
		bson.D{
			{Key: "$match",
				Value: bson.D{
					{Key: "$text",
						Value: bson.D{
							{Key: "$search", Value: "\"" + text + "\""},
							{Key: "$diacriticSensitive", Value: true},
						},
					},
				},
			},
		},
		bson.D{{Key: "$addFields", Value: bson.D{
			{Key: "textScore", Value: bson.D{{Key: "$meta", Value: "textScore"}}},
		}}},
		bson.D{{Key: "$sort", Value: bson.D{{Key: "textScore", Value: -1}}}},
		bson.D{{Key: "$limit", Value: limit}},
		bson.D{
			{Key: "$group",
				Value: bson.D{
					{Key: "_id", Value: "$wordId"},
					{Key: "examples",
						Value: bson.D{
							{Key: "$addToSet",
								Value: bson.D{
									{Key: "example", Value: "$example"},
									{Key: "defPairIndex", Value: "$defPairIndex"},
									{Key: "defEntryIndex", Value: "$defEntryIndex"},
									{Key: "exampleIndex", Value: "$exampleIndex"},
									{Key: "textScore", Value: "$textScore"},
								},
							},
						},
					},
				},
			},
		},
		bson.D{
			{Key: "$lookup",
				Value: bson.D{
					{Key: "from", Value: "words2"},
					{Key: "localField", Value: "_id"},
					{Key: "foreignField", Value: "_id"},
					{Key: "as", Value: "word"},
				},
			},
		},
		bson.D{
			{Key: "$unwind",
				Value: bson.D{
					{Key: "path", Value: "$word"},
					{Key: "preserveNullAndEmptyArrays", Value: false},
				},
			},
		},
	})
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	var result []WordExamples
	err = cursor.All(ctx, &result)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	return result, nil
}

func (m *Repository) CreateWordExamplesTextIndexIfNeeded(ctx context.Context) error {
	cursor, err := m.WordExampleCollection.Indexes().List(ctx)

	if err != nil {
		return logger.WrapError(ctx, err)
	}

	var result []bson.M
	if err = cursor.All(ctx, &result); err != nil {
		return logger.WrapError(ctx, err)
	}

	var textIndexName = "search_index"
	for i, _ := range result {
		if name, ok := result[i]["name"]; ok {
			if name == textIndexName {
				return nil
			}
		}
	}

	indexModel := mongo.IndexModel{
		Keys: bson.D{
			{Key: "example", Value: "text"},
		},
		Options: &options.IndexOptions{
			Name: &textIndexName,
			Weights: bson.D{
				{Key: "example", Value: 1},
			},
		},
	}
	_, err = m.WordExampleCollection.Indexes().CreateOne(ctx, indexModel)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	return nil
}
