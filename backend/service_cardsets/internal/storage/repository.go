package storage

import (
	"api"
	"context"
	"net/http"
	"time"
	"tools"
	"tools/logger"
	"tools/mongowrapper"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"

	"service_cardsets/internal/model"
)

var zeroTime = time.Time{}

type Repository struct {
	Logger            *logger.Logger
	MongoClient       *mongo.Client
	CardSetCollection *mongo.Collection
}

func New(logger *logger.Logger, mongoClient *mongo.Client) *Repository {
	model := &Repository{
		Logger:            logger,
		MongoClient:       mongoClient,
		CardSetCollection: mongoClient.Database(mongowrapper.MongoDatabaseCardSets).Collection(mongowrapper.MongoCollectionCardSets),
	}

	return model
}

func (m *Repository) DropAll(context context.Context) error {
	return m.CardSetCollection.Drop(context)
}

func (m *Repository) FindCardSetByCreationId(
	context context.Context,
	creationId string,
) (*model.DbCardSet, error) {
	var result model.DbCardSet
	err := m.CardSetCollection.FindOne(
		context,
		bson.M{
			"creationId": creationId,
			"isDeleted":  bson.M{"$not": bson.M{"$eq": true}},
		},
	).Decode(&result)
	if err == mongo.ErrNoDocuments {
		return nil, nil
	} else if err != nil {
		return nil, err
	}

	return &result, nil
}

func (m *Repository) DeleteCardSet(
	ctx context.Context,
	id *primitive.ObjectID,
) error {
	_, err := m.CardSetCollection.DeleteOne(ctx, bson.M{"_id": id})
	if err != nil {
		return err
	}

	return nil
}

func (m *Repository) UpdateCardSet(
	ctx context.Context,
	cardSet *api.CardSet,
) *tools.ErrorWithCode {
	for _, c := range cardSet.Cards {
		if len(c.Id) == 0 {
			c.Id = primitive.NewObjectID().Hex()
		}
		if len(c.UserId) == 0 {
			c.UserId = cardSet.UserId
		}
	}

	newCardSetDb, err := model.ApiCardSetToDb(cardSet)
	if err != nil {
		return tools.NewErrorWithCode(err, http.StatusBadRequest)
	}

	err = m.replaceCardSet(ctx, newCardSetDb)
	if err != nil {
		return tools.NewErrorWithCode(err, http.StatusInternalServerError)
	}

	return nil
}

func (m *Repository) LoadCardSetDbById(
	ctx context.Context,
	id string,
) (*model.DbCardSet, error) {
	cardSetDbId, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		return nil, err
	}

	return m.LoadCardSetDbByObjectID(ctx, cardSetDbId)
}

func (m *Repository) LoadCardSetDbByObjectID(
	ctx context.Context,
	id primitive.ObjectID,
) (*model.DbCardSet, error) {
	return m.loadCardSetDb(ctx, bson.M{"_id": id})
}

func (m *Repository) LoadCardSetDbByCreationId(
	context context.Context,
	creationId string,
) (*model.DbCardSet, error) {
	return m.loadCardSetDb(context, bson.M{"creationId": creationId})
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

func (m *Repository) InsertCardSet(
	ctx context.Context,
	cardSet *api.CardSet,
	userId *primitive.ObjectID,
) (*api.CardSet, *tools.ErrorWithCode) {
	cardSet.UserId = userId.Hex()
	for _, c := range cardSet.Cards {
		if len(c.Id) == 0 {
			c.Id = primitive.NewObjectID().Hex()
		}
		if len(c.UserId) == 0 {
			c.UserId = cardSet.UserId
		}
	}

	cardSetDb, err := model.ApiCardSetToDb(cardSet)
	if err != nil {
		return nil, tools.NewErrorWithCode(err, http.StatusBadRequest)
	}

	res, err := m.CardSetCollection.InsertOne(ctx, cardSetDb)
	if err != nil {
		return nil, tools.NewErrorWithCode(err, http.StatusInternalServerError)
	}

	objId := res.InsertedID.(primitive.ObjectID)
	cardSetDb.Id = &objId
	cardSet.Id = objId.Hex()

	return cardSet, nil
}

func (m *Repository) replaceCardSet(
	ctx context.Context,
	cardSetDb *model.DbCardSet,
) error {
	res, err := m.CardSetCollection.ReplaceOne(ctx, bson.M{"_id": cardSetDb.Id}, cardSetDb)
	if err != nil {
		return err
	}

	if res.MatchedCount == 0 {
		return mongo.ErrNoDocuments
	}

	return nil
}

func (m *Repository) HasModificationsSince(
	ctx context.Context,
	userId *primitive.ObjectID,
	date time.Time,
) (bool, error) {
	res := m.CardSetCollection.FindOne(
		ctx,
		bson.M{"userId": userId, "modificationDate": bson.M{"$gt": date}},
	)
	err := res.Err()

	if err == mongo.ErrNoDocuments {
		return false, nil

	} else if err != nil {
		return false, res.Err()
	}

	return true, nil
}

func (m *Repository) ModifiedCardSetsSince(
	ctx context.Context,
	userId *primitive.ObjectID,
	lastModificationDate *time.Time,
) ([]*model.DbCardSet, error) {
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
	if userId != nil {
		filter["userId"] = userId
	}
	cursor, err := m.CardSetCollection.Find(
		ctx,
		filter,
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

	return dbCardSets, nil
}

func (m *Repository) DeleteByIds(
	ctx context.Context,
	ids []primitive.ObjectID,
) error {
	_, err := m.CardSetCollection.DeleteMany(ctx, bson.M{"_id": bson.M{"$in": ids}})
	if err != nil {
		return err
	}

	return nil
}

func (m *Repository) MarkAsDeletedByIds(
	ctx context.Context,
	ids []primitive.ObjectID,
	modificationDate time.Time,
) error {
	_, err := m.CardSetCollection.UpdateMany(
		ctx,
		bson.M{"_id": bson.M{"$in": ids}},
		bson.M{"$set": bson.M{"isDeleted": true, "modificationDate": modificationDate}},
	)
	if err != nil {
		return err
	}

	return nil
}

func (m *Repository) IdsNotInList(
	ctx context.Context,
	ids []primitive.ObjectID,
) ([]primitive.ObjectID, error) {
	cursor, err := m.CardSetCollection.Find(
		ctx,
		bson.M{"_id": bson.M{"$not": bson.M{"$in": ids}}, "isDeleted": bson.M{"$not": bson.M{"$eq": true}}},
		&options.FindOptions{
			Projection: bson.M{"_id": 1},
		},
	)
	if err != nil {
		return []primitive.ObjectID{}, err
	}

	var result MongoIdWrapperList
	err = cursor.All(ctx, &result)
	if err != nil {
		return nil, err
	}

	return result.toMongoIds(), err
}

func (m *Repository) CardSetsNotInList(
	ctx context.Context,
	ids []*primitive.ObjectID,
) ([]*api.CardSet, error) {
	var cardSetDbs []*model.DbCardSet
	cursor, err := m.CardSetCollection.Find(
		ctx,
		bson.M{"_id": bson.M{"$not": bson.M{"$in": ids}}, "isDeleted": bson.M{"$not": bson.M{"$eq": true}}},
	)
	if err != nil {
		return nil, err
	}

	defer func() { cursor.Close(ctx) }()

	err = cursor.All(ctx, &cardSetDbs)
	if err != nil {
		return nil, err
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

func (m *Repository) CardCardSetIds(
	ctx context.Context,
	userId *primitive.ObjectID,
) ([]string, error) {
	cursor, err := m.CardSetCollection.Find(
		ctx,
		bson.M{"userId": userId, "isDeleted": bson.M{"$not": bson.M{"$eq": true}}},
		&options.FindOptions{
			Projection: bson.M{"_id": 1},
		},
	)
	if err != nil {
		return nil, err
	}

	defer func() { cursor.Close(ctx) }()

	var cardSetDbIds2 MongoIdWrapperList
	err = cursor.All(ctx, &cardSetDbIds2)
	if err != nil {
		return nil, err
	}

	cardSetApiIds := tools.Map(cardSetDbIds2.toMongoIds(), func(cardSetDbId primitive.ObjectID) string {
		return cardSetDbId.Hex()
	})

	return cardSetApiIds, nil
}
