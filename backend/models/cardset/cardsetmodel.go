package cardset

import (
	"context"
	"errors"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"models/card"
	"models/logger"
	"models/mongowrapper"
	"models/tools"
)

type CardSetModel struct {
	Logger            *logger.Logger
	MongoClient       *mongo.Client
	CardSetCollection *mongo.Collection
	CardModel         *card.CardModel
}

func New(logger *logger.Logger, mongoClient *mongo.Client, cardSetDatabase *mongo.Database, cardModel *card.CardModel) *CardSetModel {
	model := &CardSetModel{
		Logger:            logger,
		MongoClient:       mongoClient,
		CardSetCollection: cardSetDatabase.Collection(mongowrapper.MongoCollectionCardSets),
		CardModel:         cardModel,
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

func (m *CardSetModel) DeleteCardSetByCreationId(
	ctx context.Context, // transaction is required
	creationId string,
) error {
	if len(creationId) == 0 {
		return errors.New("DeleteCardSetByCreationId: empty creationId")
	}

	return m.deleteCardSet(ctx, bson.M{"creationId": creationId})
}

func (m *CardSetModel) DeleteCardSetById(
	ctx context.Context, // transaction is required
	id *primitive.ObjectID,
) error {
	return m.deleteCardSet(ctx, bson.M{"_id": id})
}

func (m *CardSetModel) deleteCardSet(
	context context.Context, // transaction is required
	filter interface{},
) error {
	// delete cards first
	cardSetDb, err := m.loadCardSetDb(context, filter)
	if err != mongo.ErrNoDocuments && err != nil {
		return err
	}

	if cardSetDb != nil {
		err = m.CardModel.DeleteByIds(context, cardSetDb.Cards)
		if err != mongo.ErrNoDocuments && err != nil {
			return err
		}
	}

	_, err = m.CardSetCollection.DeleteMany(context, filter)
	if err != mongo.ErrNoDocuments {
		return err
	}

	return nil
}

func (m *CardSetModel) LoadCardSetApiFromDb(
	context context.Context,
	cardSetDb *CardSetDb,
) (*CardSetApi, error) {
	var ids []*primitive.ObjectID
	for _, id := range cardSetDb.Cards {
		ids = append(ids, id)
	}

	cardsDb, err := m.CardModel.LoadByIds(context, ids)
	if err != nil {
		return nil, err
	}

	var cardsApi []*card.CardApi
	for _, cardDb := range cardsDb {
		cardsApi = append(cardsApi, cardDb.ToApi())
	}

	return cardSetDb.ToApi(cardsApi), nil
}

func (m *CardSetModel) UpdateCardSet(
	ctx context.Context, // transaction is required
	cardSet *CardSetApi,
) error {

	cardSetId, err := primitive.ObjectIDFromHex(cardSet.Id)
	if err != nil {
		return err
	}

	userId, err := primitive.ObjectIDFromHex(cardSet.UserId)
	if err != nil {
		return err
	}

	cardSetDb, err := m.LoadCardSetDbById(ctx, &cardSetId)
	if err != nil {
		return err
	}

	cardApis, err := m.CardModel.SyncCards(ctx, cardSet.Cards, cardSetDb.Cards, &userId)
	if err != nil {
		return err
	}

	cardIds, err := tools.MapOrError(cardApis, func(c *card.CardApi) (*primitive.ObjectID, error) {
		id, mapErr := primitive.ObjectIDFromHex(c.Id)
		return &id, mapErr
	})

	newCardSetDb, err := m.createCardSetDb(
		cardSet,
		cardIds,
	)
	if err != nil {
		return err
	}

	err = m.replaceCardSet(ctx, newCardSetDb)
	if err != nil {
		return err
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
	ctx context.Context, // transaction is required
	cardSet *CardSetApi,
	userId *primitive.ObjectID,
) (*CardSetApi, error) {

	var cardDbIds []*primitive.ObjectID
	for _, crd := range cardSet.Cards {
		cardDb, err := m.CardModel.Insert(ctx, crd, userId)
		if err != nil {
			return nil, err
		}

		crd.Id = cardDb.ID.Hex()
		cardDbIds = append(cardDbIds, cardDb.ID)
	}

	cardSetDb, err := m.createCardSetDb(cardSet, cardDbIds)
	if err != nil {
		return nil, err
	}

	res, err := m.CardSetCollection.InsertOne(ctx, cardSetDb)
	if err != nil {
		return nil, err
	}

	objId := res.InsertedID.(primitive.ObjectID)
	cardSetDb.ID = &objId
	cardSet.Id = objId.Hex()
	cardSet.UserId = userId.Hex()

	return cardSet, nil
}

func (m *CardSetModel) replaceCardSet(
	ctx context.Context, // transaction is required
	cardSetDb *CardSetDb,
) error {
	_, err := m.CardSetCollection.ReplaceOne(ctx, bson.M{"_id": cardSetDb.ID}, cardSetDb)
	if err != nil {
		return err
	}

	return nil
}

func (m *CardSetModel) createCardSetDb(
	cardSet *CardSetApi,
	cardDbIds []*primitive.ObjectID,
) (*CardSetDb, error) {
	userId, err := primitive.ObjectIDFromHex(cardSet.UserId)
	if err != nil {
		return nil, err
	}

	creationDate, err := tools.ApiDateToDbDate(cardSet.CreationDate)
	if err != nil {
		return nil, err
	}

	modificationDateTime, err := tools.ApiDatePtrToDbDatePtr(cardSet.ModificationDate)
	if err != nil {
		return nil, err
	}

	cardSetDb := &CardSetDb{
		Name:             cardSet.Name,
		Cards:            cardDbIds,
		UserId:           &userId,
		CreationDate:     creationDate,
		ModificationDate: modificationDateTime,
		CreationId:       cardSet.CreationId,
	}
	return cardSetDb, nil
}
