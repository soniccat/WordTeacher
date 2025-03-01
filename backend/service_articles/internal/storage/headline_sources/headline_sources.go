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
	cursor, err := m.HeadlineSourceCollection.Find(
		ctx,
		bson.M{
			"nextCrawlDate": bson.M{"$lte": currentTime},
		},
		options.Find().SetSort(bson.M{"nextCrawlDate": -1}),
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

type nextCrawlDateDateWrapper struct {
	NextCrawlDate primitive.DateTime `bson:"nextCrawlDate"`
}
type nextCrawlDateDateWrapperList []nextCrawlDateDateWrapper

func (m *Storage) InsertHeadlineSource(
	ctx context.Context,
	headlineSource *model.HeadlineSource,
) error {
	res, err := m.HeadlineSourceCollection.InsertOne(ctx, headlineSource)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	objId := res.InsertedID.(primitive.ObjectID)
	headlineSource.Id = objId.Hex()

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
