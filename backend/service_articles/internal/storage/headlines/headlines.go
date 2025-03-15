package headlines

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
	MongoDatabaseHeadlines    = "headlines"
	MongoCollectionHeadlines  = "headlines"
	MaxHeadlineCountPerSource = 5000
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
	r.MongoWrapper.CreateIndexIfNeeded(r.HeadlineCollection, "date")

	return r, nil
}

func (m *Storage) DropAll(ctx context.Context) error {
	return logger.WrapError(ctx, m.HeadlineCollection.Drop(ctx))
}

func (m *Storage) FindHeadlines(
	ctx context.Context,
	category string,
	limit int64,
	since *time.Time,
) ([]model.Headline, error) {
	filter := bson.M{}
	if category != "" && category != model.HeadlineSourceCategoryAll {
		filter["sourceCategory"] = category
	}
	if since != nil {
		filter["date"] = bson.M{"$gte": since}
	}

	cursor, err := m.HeadlineCollection.Find(
		ctx,
		filter,
		options.Find().SetLimit(limit).
			SetSort(
				bson.D{
					{Key: "date", Value: -1},
				},
			),
	)
	if errors.Is(err, mongo.ErrNoDocuments) {
		return nil, nil
	} else if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	var result []model.Headline
	err = cursor.All(ctx, &result)
	if err != nil {
		return nil, err
	}

	return result, nil
}

func (m *Storage) FindLatestHeadline(
	ctx context.Context,
	sourceId string,
) (*model.Headline, error) {
	cursor, err := m.HeadlineCollection.Find(
		ctx,
		bson.M{
			"sourceId": sourceId,
		},
		options.Find().SetLimit(1).
			SetSort(
				bson.D{
					{Key: "date", Value: -1},
				},
			),
	)
	if errors.Is(err, mongo.ErrNoDocuments) {
		return nil, nil
	} else if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	var result []model.Headline
	err = cursor.All(ctx, &result)
	if err != nil {
		return nil, err
	}

	if len(result) == 0 {
		return nil, nil
	}

	return &result[0], nil
}

func (m *Storage) InsertHeadlines(
	ctx context.Context,
	headlines []model.Headline,
) error {
	if len(headlines) == 0 {
		return nil
	}

	var headlinesAsInterfaces []any
	for i, _ := range headlines {
		headlinesAsInterfaces = append(headlinesAsInterfaces, headlines[i])
	}

	res, err := m.HeadlineCollection.InsertMany(
		ctx,
		headlinesAsInterfaces,
		&options.InsertManyOptions{},
	)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	for i, _ := range headlines {
		objId := res.InsertedIDs[i].(primitive.ObjectID)
		headlines[i].Id = objId.Hex()
	}

	return nil
}

func (m *Storage) KeepRecentHeadlines(
	ctx context.Context,
	sourceId string,
	count int64,
) error {
	cursor, err := m.HeadlineCollection.Find(
		ctx,
		bson.M{
			"sourceId": sourceId,
		},
		options.Find().SetSort(bson.D{
			{Key: "date", Value: -1},
		}).SetProjection(bson.M{
			"_id": 1,
		}),
	)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	var headlineIds mongowrapper.MongoStringIdWrapperList
	err = cursor.All(ctx, &headlineIds)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	if len(headlineIds) > MaxHeadlineCountPerSource {
		headlineIdsToRemove := headlineIds[MaxHeadlineCountPerSource:]
		err = m.DeleteHeadlinesByIds(ctx, tools.Map(headlineIdsToRemove, func(w mongowrapper.MongoStringIdWrapper) string {
			return w.Id
		}))
		if err != nil {
			return err
		}
	}

	return nil
}

func (m *Storage) DeleteHeadlinesByIds(
	ctx context.Context,
	ids []string,
) error {
	if len(ids) == 0 {
		return nil
	}

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
