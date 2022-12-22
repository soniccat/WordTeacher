package cardset

import (
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"models/apphelpers"
	"models/logger"
	"models/mongowrapper"
	"models/tools"
	"net/http"
	"time"
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
		CardSetCollection: mongoClient.Database(mongowrapper.MongoDatabaseCardSets).Collection(mongowrapper.MongoCollectionCardSets),
	}

	return model
}

func (m *Repository) FindCardSetByCreationId(
	context context.Context,
	creationId string,
) (*CardSetDb, error) {
	var result CardSetDb
	err := m.CardSetCollection.FindOne(context, bson.M{"creationId": creationId}).Decode(&result)
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
	cardSet *CardSetApi,
) *apphelpers.ErrorWithCode {
	for _, c := range cardSet.Cards {
		if len(c.Id) == 0 {
			c.Id = primitive.NewObjectID().Hex()
		}
		if len(c.UserId) == 0 {
			c.UserId = cardSet.UserId
		}
	}

	newCardSetDb, err := cardSet.toDb()
	if err != nil {
		return apphelpers.NewErrorWithCode(err, http.StatusBadRequest)
	}

	err = m.replaceCardSet(ctx, newCardSetDb)
	if err != nil {
		return apphelpers.NewErrorWithCode(err, http.StatusInternalServerError)
	}

	return nil
}

func (m *Repository) LoadCardSetDbById(
	context context.Context,
	id *primitive.ObjectID,
) (*CardSetDb, error) {
	return m.loadCardSetDb(context, bson.M{"_id": id})
}

func (m *Repository) LoadCardSetDbByCreationId(
	context context.Context,
	creationId string,
) (*CardSetDb, error) {
	return m.loadCardSetDb(context, bson.M{"creationId": creationId})
}

func (m *Repository) loadCardSetDb(
	context context.Context,
	filter interface{},
) (*CardSetDb, error) {
	var cardSetDb CardSetDb
	err := m.CardSetCollection.FindOne(context, filter).Decode(&cardSetDb)
	if err != nil {
		return nil, err
	}

	return &cardSetDb, nil
}

func (m *Repository) InsertCardSet(
	ctx context.Context,
	cardSet *CardSetApi,
	userId *primitive.ObjectID,
) (*CardSetApi, *apphelpers.ErrorWithCode) {
	cardSet.UserId = userId.Hex()
	for _, c := range cardSet.Cards {
		if len(c.Id) == 0 {
			c.Id = primitive.NewObjectID().Hex()
		}
		if len(c.UserId) == 0 {
			c.UserId = cardSet.UserId
		}
	}

	cardSetDb, err := cardSet.toDb()
	if err != nil {
		return nil, apphelpers.NewErrorWithCode(err, http.StatusBadRequest)
	}

	res, err := m.CardSetCollection.InsertOne(ctx, cardSetDb)
	if err != nil {
		return nil, apphelpers.NewErrorWithCode(err, http.StatusInternalServerError)
	}

	objId := res.InsertedID.(primitive.ObjectID)
	cardSetDb.Id = &objId
	cardSet.Id = objId.Hex()

	return cardSet, nil
}

func (m *Repository) replaceCardSet(
	ctx context.Context,
	cardSetDb *CardSetDb,
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
	date time.Time,
) (bool, error) {
	dbTime := primitive.NewDateTimeFromTime(date)
	res := m.CardSetCollection.FindOne(
		ctx,
		bson.M{"modificationDate": bson.M{"$gt": dbTime}},
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
	date time.Time,
) ([]*CardSetApi, error) {
	dbTime := primitive.NewDateTimeFromTime(date)
	cursor, err := m.CardSetCollection.Find(
		ctx,
		bson.M{"modificationDate": bson.M{"$gt": dbTime}},
	)
	if err != nil {
		return nil, err
	}

	defer func() { cursor.Close(ctx) }()

	var cardSetsDb []*CardSetDb
	err = cursor.All(ctx, &cardSetsDb)
	if err != nil {
		return nil, err
	}

}

func (m *Repository) DeleteNotInList(
	ctx context.Context,
	ids []*primitive.ObjectID,
) error {
	_, err := m.CardSetCollection.DeleteMany(ctx, bson.M{"_id": bson.M{"$not": bson.M{"$in": ids}}})
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

func (m *Repository) CardSetsNotInList(
	ctx context.Context,
	ids []*primitive.ObjectID,
) ([]*CardSetApi, error) {
	var cardSetDbs []*CardSetDb
	cursor, err := m.CardSetCollection.Find(ctx, bson.M{"_id": bson.M{"$not": bson.M{"$in": ids}}})
	if err != nil {
		return nil, err
	}

	defer func() { cursor.Close(context) }()

	err = cursor.All(ctx, &cardSetDbs)
	if err != nil {
		return nil, err
	}

	cardSetApis := tools.Map(cardSetDbs, func(cardSetDb *CardSetDb) *CardSetApi {
		return cardSetDb.ToApi()
	})

	return cardSetApis, nil
}

func (m *Repository) CardCardSetIds(
	ctx context.Context,
	userId *primitive.ObjectID,
) ([]string, error) {
	var cardSetDbIds []*primitive.ObjectID
	cursor, err := m.CardSetCollection.Find(
		ctx,
		bson.M{"userId": userId},
		&options.FindOptions{
			Projection: bson.M{"_id": 1},
		},
	)
	if err != nil {
		return nil, err
	}

	defer func() { cursor.Close(ctx) }()

	err = cursor.All(ctx, &cardSetDbIds)
	if err != nil {
		return nil, err
	}

	cardSetApiIds := tools.Map(cardSetDbIds, func(cardSetDbId *primitive.ObjectID) string {
		return cardSetDbId.Hex()
	})

	return cardSetApiIds, nil
}
