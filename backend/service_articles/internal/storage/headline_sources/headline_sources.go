package headline_sources

import (
	"context"
	"errors"
	"service_articles/internal/model"
	"service_articles/internal/storage/headlines"
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
	MongoCollectionHeadlineSources = "headline_sources"
)

type Storage struct {
	mongowrapper.MongoEnv
	Logger                   *logger.Logger
	HeadlineSourceCollection *mongo.Collection
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

	r.HeadlineSourceCollection = r.Collection(headlines.MongoDatabaseHeadlines, MongoCollectionHeadlineSources)
	r.MongoWrapper.CreateIndexIfNeeded(r.HeadlineSourceCollection, "nextCrawlDate")

	return r, nil
}

func (m *Storage) DropAll(ctx context.Context) error {
	return logger.WrapError(ctx, m.HeadlineSourceCollection.Drop(ctx))
}

func (m *Storage) FindSourcesReadyToCrawl(
	ctx context.Context,
	currentTime time.Time,
) ([]model.HeadlineSource, error) {
	return m.FindSources(
		ctx,
		bson.M{
			"$or": []any{
				bson.M{"nextCrawlDate": bson.M{"$lte": currentTime}},
				bson.M{"nextCrawlDate": nil},
			},
		},
		options.Find().SetSort(bson.M{"nextCrawlDate": -1}),
	)
}

func (m *Storage) AllSources(
	ctx context.Context,
) ([]model.HeadlineSource, error) {
	return m.FindSources(
		ctx,
		bson.M{},
		options.Find().SetSort(bson.M{"nextCrawlDate": -1}),
	)
}

func (m *Storage) FindSources(
	ctx context.Context,
	filter interface{},
	opts ...*options.FindOptions,
) ([]model.HeadlineSource, error) {
	cursor, err := m.HeadlineSourceCollection.Find(
		ctx,
		filter,
		opts...,
	)
	if errors.Is(err, mongo.ErrNoDocuments) {
		return nil, nil
	} else if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	var result []model.HeadlineSource
	err = cursor.All(ctx, &result)
	if err != nil {
		return nil, err
	}

	return result, nil
}

func (m *Storage) FindNextCrawlDate(
	ctx context.Context,
) (*time.Time, error) {
	cursor, err := m.HeadlineSourceCollection.Find(
		ctx,
		bson.M{},
		options.Find().SetProjection(bson.M{"nextCrawlDate": 1}).SetLimit(int64(1)).SetSort(bson.M{"nextCrawlDate": -1}),
	)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	defer func() { cursor.Close(ctx) }()

	var modificationDates nextCrawlDateDateWrapperList
	err = cursor.All(ctx, &modificationDates)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	if len(modificationDates) == 0 {
		return nil, nil
	}

	return tools.Ptr(modificationDates[0].NextCrawlDate.Time().UTC()), nil
}

func (m *Storage) UpdateCrawlDate(
	ctx context.Context,
	id string,
	newLastCrawlDate time.Time,
	newNextCrawDate time.Time,
) error {
	mongoId, err := tools.ParseObjectID(ctx, id)
	if err != nil {
		return err
	}

	_, err = m.HeadlineSourceCollection.UpdateOne(
		ctx,
		bson.M{
			"_id": mongoId,
		},
		bson.M{
			"$set": bson.M{
				"lastCrawlDate": newLastCrawlDate,
				"nextCrawlDate": newNextCrawDate,
			},
		},
		&options.UpdateOptions{},
	)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	return nil
}

type nextCrawlDateDateWrapper struct {
	NextCrawlDate primitive.DateTime `bson:"nextCrawlDate"`
}
type nextCrawlDateDateWrapperList []nextCrawlDateDateWrapper

func (m *Storage) InsertHeadlineSources(
	ctx context.Context,
	headlineSources []model.HeadlineSource,
) error {
	if len(headlineSources) == 0 {
		return nil
	}

	var headlineSourcesAsInterfaces []any
	for i, _ := range headlineSources {
		headlineSourcesAsInterfaces = append(headlineSourcesAsInterfaces, headlineSources[i])
	}

	res, err := m.HeadlineSourceCollection.InsertMany(ctx, headlineSourcesAsInterfaces, &options.InsertManyOptions{})
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	for i, _ := range headlineSources {
		objId := res.InsertedIDs[i].(primitive.ObjectID)
		headlineSources[i].Id = objId.Hex()
	}

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

	_, err = m.HeadlineSourceCollection.DeleteMany(
		ctx, bson.M{
			"_id": bson.M{"$in": mongoIds},
		},
	)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	return nil
}
