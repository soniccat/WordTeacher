package cardsetsearch

import (
	"api"
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"net/http"
	"tools"
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
		CardSetCollection: mongoClient.Database(mongowrapper.MongoDatabaseCardSets).Collection(mongowrapper.MongoCollectionCardSets),
	}

	return model
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

	newCardSetDb, err := ApiCardSetToDb(cardSet)
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
	context context.Context,
	id string,
) (*DbCardSet, error) {
	cardSetDbId, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		return nil, err
	}
	return m.loadCardSetDb(context, bson.M{"_id": cardSetDbId})
}

func (m *Repository) loadCardSetDb(
	context context.Context,
	filter interface{},
) (*DbCardSet, error) {
	var cardSetDb DbCardSet
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

	cardSetDb, err := ApiCardSetToDb(cardSet)
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
	cardSetDb *DbCardSet,
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
