package storage

import (
	"context"
	"errors"
	"time"
	"tools"
	"tools/logger"
	"tools/mongowrapper"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

var zeroTime = time.Time{}

type Storage struct {
	mongowrapper.MongoEnv
	Logger            *logger.Logger
	CardSetCollection *mongo.Collection
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

	r.CardSetCollection = r.Collection(mongowrapper.MongoDatabaseCardSets, mongowrapper.MongoCollectionCardSets)

	return r, nil
}

func (m *Storage) DropAll(ctx context.Context) error {
	return logger.WrapError(ctx, m.CardSetCollection.Drop(ctx))
}

func (m *Storage) FindCardSetByCreationId(
	ctx context.Context,
	creationId string,
) (*model.DbCardSet, error) {
	var result model.DbCardSet
	err := m.CardSetCollection.FindOne(
		ctx,
		bson.M{
			"creationId": creationId,
			"isDeleted":  bson.M{"$not": bson.M{"$eq": true}},
		},
	).Decode(&result)
	if errors.Is(err, mongo.ErrNoDocuments) {
		return nil, nil
	} else if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	return &result, nil
}

func (m *Storage) DeleteCardSet(
	ctx context.Context,
	id *primitive.ObjectID,
) error {
	_, err := m.CardSetCollection.DeleteOne(ctx, bson.M{"_id": id})
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	return nil
}

func (m *Storage) UpdateCardSet(
	ctx context.Context,
	cardSet *api.CardSet,
) error {
	for _, c := range cardSet.Cards {
		if len(c.Id) == 0 {
			c.Id = primitive.NewObjectID().Hex()
		}
		if len(c.UserId) == 0 {
			c.UserId = cardSet.UserId
		}
	}

	newCardSetDb, err := model.ApiCardSetToDb(ctx, cardSet)
	if err != nil {
		return logger.WrapError(ctx, tools.NewInvalidArgumentError("UpdateCardSet.cardSet", cardSet, "", err))
	}

	err = m.replaceCardSet(ctx, newCardSetDb)
	if err != nil {
		return err
	}

	return nil
}

func (m *Storage) LoadCardSetDbById(
	ctx context.Context,
	id string,
) (*model.DbCardSet, error) {
	cardSetDbId, err := tools.ParseObjectID(ctx, id)
	if err != nil {
		return nil, err
	}

	return m.loadCardSetDbByObjectID(ctx, *cardSetDbId)
}

func (m *Storage) loadCardSetDbByObjectID(
	ctx context.Context,
	id primitive.ObjectID,
) (*model.DbCardSet, error) {
	return m.loadCardSetDb(ctx, bson.M{"_id": id})
}

func (m *Storage) LoadCardSetDbByCreationId(
	context context.Context,
	creationId string,
) (*model.DbCardSet, error) {
	return m.loadCardSetDb(context, bson.M{"creationId": creationId})
}

func (m *Storage) loadCardSetDb(
	ctx context.Context,
	filter interface{},
) (*model.DbCardSet, error) {
	var cardSetDb model.DbCardSet
	err := m.CardSetCollection.FindOne(ctx, filter).Decode(&cardSetDb)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	return &cardSetDb, nil
}

func (m *Storage) InsertCardSet(
	ctx context.Context,
	cardSet *api.CardSet,
	userId string,
) (*api.CardSet, error) {
	cardSet.UserId = userId
	for _, c := range cardSet.Cards {
		if len(c.Id) == 0 {
			c.Id = primitive.NewObjectID().Hex()
		}
		if len(c.UserId) == 0 {
			c.UserId = cardSet.UserId
		}
	}

	cardSetDb, err := model.ApiCardSetToDb(ctx, cardSet)
	if err != nil {
		return nil, tools.NewInvalidArgumentError("InsertCardSet.cardSet", cardSet, "", err)
	}

	res, err := m.CardSetCollection.InsertOne(ctx, cardSetDb)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	objId := res.InsertedID.(primitive.ObjectID)
	cardSetDb.Id = &objId
	cardSet.Id = objId.Hex()

	return cardSet, nil
}

func (m *Storage) replaceCardSet(
	ctx context.Context,
	cardSetDb *model.DbCardSet,
) error {
	res, err := m.CardSetCollection.ReplaceOne(ctx, bson.M{"_id": cardSetDb.Id}, cardSetDb)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	if res.MatchedCount == 0 {
		return logger.WrapError(ctx, mongo.ErrNoDocuments)
	}

	return nil
}

func (m *Storage) HasModificationsSince(
	ctx context.Context,
	userId string,
	lastModificationDate *time.Time,
) (bool, error) {
	dbUserId, err := tools.ParseObjectID(ctx, userId)
	if err != nil {
		return false, err
	}

	var date time.Time
	if lastModificationDate != nil {
		date = *lastModificationDate
	} else {
		date = zeroTime
	}

	res := m.CardSetCollection.FindOne(
		ctx,
		bson.M{"userId": dbUserId, "modificationDate": bson.M{"$gt": date}},
	)
	err = res.Err()

	if errors.Is(err, mongo.ErrNoDocuments) {
		return false, nil

	} else if err != nil {
		return false, logger.WrapError(ctx, err)
	}

	return true, nil
}

func (m *Storage) ModifiedCardSetsSinceByUserId(
	ctx context.Context,
	userId string,
	lastModificationDate *time.Time,
) ([]*model.DbCardSet, error) {
	return m.modifiedCardSetsSince(ctx, &userId, lastModificationDate)
}

func (m *Storage) ModifiedCardSetsSince(
	ctx context.Context,
	lastModificationDate *time.Time,
) ([]*model.DbCardSet, error) {
	return m.modifiedCardSetsSince(ctx, nil, lastModificationDate)
}

func (m *Storage) modifiedCardSetsSince(
	ctx context.Context,
	userId *string,
	lastModificationDate *time.Time,
) ([]*model.DbCardSet, error) {
	var mongoUserId *primitive.ObjectID

	if userId != nil {
		muid, err := tools.ParseObjectID(ctx, *userId)
		if err != nil {
			return nil, err
		}

		mongoUserId = muid
	}

	var date time.Time
	if lastModificationDate != nil {
		date = *lastModificationDate
	} else {
		date = zeroTime
	}

	dbTime := primitive.NewDateTimeFromTime(date)
	filter := bson.M{
		"modificationDate": bson.M{"$gt": dbTime},
		"isDeleted":        bson.M{"$not": bson.M{"$eq": true}},
	}
	if mongoUserId != nil {
		filter["userId"] = mongoUserId
	}
	cursor, err := m.CardSetCollection.Find(
		ctx,
		filter,
	)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	defer func() { cursor.Close(ctx) }()

	var dbCardSets []*model.DbCardSet
	err = cursor.All(ctx, &dbCardSets)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	return dbCardSets, nil
}

func (m *Storage) DeleteByIds(
	ctx context.Context,
	ids []primitive.ObjectID,
) error {
	_, err := m.CardSetCollection.DeleteMany(ctx, bson.M{"_id": bson.M{"$in": ids}})
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	return nil
}

func (m *Storage) MarkAsDeletedByIds(
	ctx context.Context,
	ids []string,
	modificationDate time.Time,
) error {
	mongoIds, err := tools.IdsToMongoIds(ctx, ids)
	if err != nil {
		return err
	}

	_, err = m.CardSetCollection.UpdateMany(
		ctx,
		bson.M{"_id": bson.M{"$in": mongoIds}},
		bson.M{"$set": bson.M{"isDeleted": true, "modificationDate": modificationDate}},
	)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	return nil
}

func (m *Storage) IdsNotInList(
	ctx context.Context,
	userId string,
	ids []string,
) ([]string, error) {
	userDbId, err := tools.ParseObjectID(ctx, userId)
	if err != nil {
		return nil, err
	}

	mongoIds, err := tools.IdsToMongoIds(ctx, ids)
	if err != nil {
		return nil, err
	}

	cursor, err := m.CardSetCollection.Find(
		ctx,
		bson.M{"_id": bson.M{"$not": bson.M{"$in": mongoIds}}, "userId": userDbId, "isDeleted": bson.M{"$not": bson.M{"$eq": true}}},
		&options.FindOptions{
			Projection: bson.M{"_id": 1},
		},
	)
	if err != nil {
		return []string{}, logger.WrapError(ctx, err)
	}

	var result MongoIdWrapperList
	err = cursor.All(ctx, &result)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	return tools.MongoIdsToStrings(result.toMongoIds()), nil
}

func (m *Storage) CardSetsNotInList(
	ctx context.Context,
	userId string,
	ids []string,
) ([]*api.CardSet, error) {
	userDbId, err := tools.ParseObjectID(ctx, userId)
	if err != nil {
		return nil, err
	}

	mongoIds, err := tools.IdsToMongoIds(ctx, ids)
	if err != nil {
		return nil, err
	}

	var cardSetDbs []*model.DbCardSet
	cursor, err := m.CardSetCollection.Find(
		ctx,
		bson.M{"_id": bson.M{"$not": bson.M{"$in": mongoIds}}, "userId": userDbId, "isDeleted": bson.M{"$not": bson.M{"$eq": true}}},
	)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	defer func() { cursor.Close(ctx) }()

	err = cursor.All(ctx, &cardSetDbs)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	return model.DbCardSetsToApi(cardSetDbs), nil
}

type MongoIdWrapper struct {
	Id primitive.ObjectID `bson:"_id"`
}

type MongoIdWrapperList []MongoIdWrapper

func (l *MongoIdWrapperList) toMongoIds() []primitive.ObjectID {
	return tools.Map[MongoIdWrapper](*l, func(t MongoIdWrapper) primitive.ObjectID {
		return t.Id
	})
}

func (m *Storage) CardCardSetIds(
	ctx context.Context,
	userId string,
) ([]string, error) {
	userDbId, err := tools.ParseObjectID(ctx, userId)
	if err != nil {
		return nil, err
	}

	cursor, err := m.CardSetCollection.Find(
		ctx,
		bson.M{"userId": userDbId, "isDeleted": bson.M{"$not": bson.M{"$eq": true}}},
		&options.FindOptions{
			Projection: bson.M{"_id": 1},
		},
	)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	defer func() { cursor.Close(ctx) }()

	var cardSetDbIds2 MongoIdWrapperList
	err = cursor.All(ctx, &cardSetDbIds2)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	cardSetApiIds := tools.Map(cardSetDbIds2.toMongoIds(), func(cardSetDbId primitive.ObjectID) string {
		return cardSetDbId.Hex()
	})

	return cardSetApiIds, nil
}

type MongoModificationDateWrapper struct {
	ModificationDate primitive.DateTime `bson:"modificationDate"`
}

type MongoModificationDateWrapperList []MongoModificationDateWrapper

func (m *Storage) LastModificationDate(
	ctx context.Context,
	userId string,
) (*time.Time, error) {
	mongoUserId, err := tools.ParseObjectID(ctx, userId)
	if err != nil {
		return nil, err
	}

	cursor, err := m.CardSetCollection.Find(
		ctx,
		bson.M{"userId": mongoUserId}, // include deleted
		options.Find().SetProjection(bson.M{"modificationDate": 1}).SetLimit(int64(1)).SetSort(bson.M{"modificationDate": -1}),
	)

	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	defer func() { cursor.Close(ctx) }()

	var modificationDates MongoModificationDateWrapperList
	err = cursor.All(ctx, &modificationDates)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	if len(modificationDates) == 0 {
		return nil, nil
	}

	return tools.Ptr(modificationDates[0].ModificationDate.Time().UTC()), nil
}
