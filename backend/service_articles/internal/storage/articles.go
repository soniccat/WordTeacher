package storage

import (
	"context"
	"errors"
	"service_articles/internal/model"
	"time"
	"tools"
	"tools/logger"
	"tools/mongowrapper"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

const (
	MongoDatabaseHeadlines   = "headlines"
	MongoCollectionHeadlines = "headlines"
)

type Storage struct {
	mongowrapper.MongoEnv
	Logger             *logger.Logger
	HeadlineCollection *mongo.Collection
}

func New(
	logger *logger.Logger,
	mongoURI string,
	enableCredentials bool,
) (*Storage, error) {
	r := &Storage{
		MongoEnv: mongowrapper.NewMongoEnv(logger),
		Logger:   logger,
	}
	err := r.SetupMongo(mongoURI, enableCredentials)
	if err != nil {
		return nil, err
	}

	r.HeadlineCollection = r.Collection(MongoDatabaseHeadlines, MongoCollectionHeadlines)
	r.MongoWrapper.CreateIndexIfNeeded(r.HeadlineCollection, "updateDate")

	return r, nil
}

func (m *Storage) DropAll(ctx context.Context) error {
	return logger.WrapError(ctx, m.HeadlineCollection.Drop(ctx))
}

func (m *Storage) FindHeadlines(
	ctx context.Context,
	since time.Time,
	limit int64,
) ([]model.Headline, error) {
	var result []model.Headline
	cursor, err := m.HeadlineCollection.Find(
		ctx,
		bson.M{
			"updateDate": bson.M{"$gte": since},
		},
		options.Find().SetLimit(limit).SetSort(bson.M{"updateDate": -1}),
	)
	if errors.Is(err, mongo.ErrNoDocuments) {
		return nil, nil
	} else if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	err = cursor.All(ctx, &result)
	if err != nil {
		return nil, err
	}

	return result, nil
}

func (m *Storage) InsertHeadline(
	ctx context.Context,
	headline *model.Headline,
) error {
	res, err := m.HeadlineCollection.InsertOne(ctx, headline)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	objId := res.InsertedID.(primitive.ObjectID)
	headline.Id = objId.Hex()

	return nil
}

func (m *Storage) DeleteHeadlinesByIds(
	ctx context.Context,
	ids []string,
) error {
	mongoIds, err := tools.IdsToMongoIds(ctx, ids)
	if err != nil {
		return err
	}

	_, err = m.HeadlineCollection.DeleteMany(
		ctx, bson.M{
			"_id": bson.M{"$in": mongoIds},
		},
	)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	return nil
}
