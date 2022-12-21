package cardset

import (
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"models/apphelpers"
	"models/logger"
	"models/mongowrapper"
	"net/http"
)

type CardSetModel struct {
	Logger            *logger.Logger
	MongoClient       *mongo.Client
	CardSetCollection *mongo.Collection
}

func New(logger *logger.Logger, mongoClient *mongo.Client) *CardSetModel {
	model := &CardSetModel{
		Logger:            logger,
		MongoClient:       mongoClient,
		CardSetCollection: mongoClient.Database(mongowrapper.MongoDatabaseCardSets).Collection(mongowrapper.MongoCollectionCardSets),
	}

	return model
}

func (m *CardSetModel) FindCardSetByCreationId(
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

func (m *CardSetModel) DeleteCardSet(
	ctx context.Context,
	id *primitive.ObjectID,
) error {
	_, err := m.CardSetCollection.DeleteOne(ctx, bson.M{"_id": id})
	if err != nil {
		return err
	}

	return nil
}

func (m *CardSetModel) UpdateCardSet(
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
		return apphelpers.NewErrorWithCode(http.StatusBadRequest, err)
	}

	err = m.replaceCardSet(ctx, newCardSetDb)
	if err != nil {
		return apphelpers.NewErrorWithCode(http.StatusInternalServerError, err)
	}

	return nil
}

func (m *CardSetModel) LoadCardSetDbById(
	context context.Context,
	id *primitive.ObjectID,
) (*CardSetDb, error) {
	return m.loadCardSetDb(context, bson.M{"_id": id})
}

func (m *CardSetModel) LoadCardSetDbByCreationId(
	context context.Context,
	creationId string,
) (*CardSetDb, error) {
	return m.loadCardSetDb(context, bson.M{"creationId": creationId})
}

func (m *CardSetModel) loadCardSetDb(
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

func (m *CardSetModel) InsertCardSet(
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
		return nil, apphelpers.NewErrorWithCode(http.StatusBadRequest, err)
	}

	res, err := m.CardSetCollection.InsertOne(ctx, cardSetDb)
	if err != nil {
		return nil, apphelpers.NewErrorWithCode(http.StatusInternalServerError, err)
	}

	objId := res.InsertedID.(primitive.ObjectID)
	cardSetDb.Id = &objId
	cardSet.Id = objId.Hex()

	return cardSet, nil
}

func (m *CardSetModel) replaceCardSet(
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
