package storage

import (
	"api"
	"context"
	"log"
	"time"
	"tools"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"

	"service_cardsetsearch/internal/model"
	"tools/logger"
	"tools/mongowrapper"
)

type Repository struct {
	Logger            *logger.Logger
	MongoClient       *mongo.Client
	CardSetCollection *mongo.Collection
}

func New(logger *logger.Logger, mongoClient *mongo.Client) *Repository {
	model := &Repository{
		Logger:            logger,
		MongoClient:       mongoClient,
		CardSetCollection: mongoClient.Database(mongowrapper.MongoDatabaseCardSetSearch).Collection(mongowrapper.MongoCollectionCardSetSearch),
	}

	return model
}

func (m *Repository) CreateTextIndexIfNeeded(ctx context.Context) error {
	cursor, err := m.CardSetCollection.Indexes().List(ctx)

	if err != nil {
		log.Fatal(err)
	}

	var result []bson.M
	if err = cursor.All(ctx, &result); err != nil {
		return err
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
			{Key: "name", Value: "text"},
			{Key: "description", Value: "text"},
			{Key: "terms", Value: "text"},
		},
		Options: &options.IndexOptions{
			Name: &textIndexName,
			Weights: bson.D{
				{Key: "name", Value: 3},
				{Key: "description", Value: 3},
				{Key: "terms", Value: 1},
			},
		},
	}
	_, err = m.CardSetCollection.Indexes().CreateOne(ctx, indexModel)
	if err != nil {
		return err
	}

	return nil
}

func (m *Repository) DeleteSearchCardSetByCardSetId(
	ctx context.Context,
	cardSetId *primitive.ObjectID,
) error {
	_, err := m.CardSetCollection.DeleteOne(ctx, bson.M{"cardSetId": cardSetId})
	if err != nil {
		return err
	}

	return nil
}

func (m *Repository) LoadCardSetDbById(
	context context.Context,
	id string,
) (*model.DbCardSet, error) {
	cardSetDbId, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		return nil, err
	}
	return m.loadCardSetDb(context, bson.M{"_id": cardSetDbId})
}

func (m *Repository) loadCardSetDb(
	context context.Context,
	filter interface{},
) (*model.DbCardSet, error) {
	var cardSetDb model.DbCardSet
	err := m.CardSetCollection.FindOne(context, filter).Decode(&cardSetDb)
	if err != nil {
		return nil, err
	}

	return &cardSetDb, nil
}

func (m *Repository) UpsertCardSet(
	ctx context.Context,
	cardSetDb *model.DbCardSet,
) error {
	_, err := m.CardSetCollection.ReplaceOne(
		ctx,
		bson.M{"cardSetId": cardSetDb.CardSetId},
		cardSetDb,
		options.Replace().SetUpsert(true),
	)

	if err != nil {
		return err
	}

	return nil
}

func (m *Repository) DeleteCardSets(
	ctx context.Context,
	ids []*primitive.ObjectID,
) error {
	_, err := m.CardSetCollection.DeleteMany(ctx, bson.M{"_id": bson.M{"$in": ids}})
	if err != nil {
		return err
	}

	return nil
}

func (m *Repository) SearchCardSets(
	ctx context.Context,
	query string,
) ([]*api.CardSet, error) {
	cursor, err := m.CardSetCollection.Find(
		ctx,
		bson.M{
			"$text": bson.M{"$search": query, "$diacriticSensitive": true},
		},
	)
	if err != nil {
		return nil, err
	}

	defer func() { cursor.Close(ctx) }()

	var dbCardSets []*model.DbCardSet
	err = cursor.All(ctx, &dbCardSets)
	if err != nil {
		return nil, err
	}

	return model.DbCardSetsToApi(dbCardSets), nil
}

type modificationDateResult struct {
	ModificationDate primitive.DateTime `bson:"modificationDate"`
}

func (m *Repository) LastCardSetModificationDate(
	ctx context.Context,
) (*time.Time, error) {
	var r modificationDateResult
	err := m.CardSetCollection.FindOne(
		ctx,
		bson.M{},
		options.FindOne().SetProjection(bson.M{"modificationDate": 1}).SetSort(bson.M{"modificationDate": -1}),
	).Decode(&r)
	if err != nil {
		return nil, err
	}

	return tools.Ptr(r.ModificationDate.Time()), nil
}
