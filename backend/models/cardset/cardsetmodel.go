package cardset

import (
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"models/card"
	"models/logger"
	"models/mongowrapper"
	"models/tools"
	"time"
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
	context context.Context,
	creationId string,
) error {
	session, err := m.MongoClient.StartSession()
	if err != nil {
		return err
	}

	defer func() {
		session.EndSession(context)
	}()

	_, err = session.WithTransaction(
		context,
		func(sCtx mongo.SessionContext) (interface{}, error) {

			// delete cards first
			cardSetDb, err := m.LoadCardSetDbByCreationId(sCtx, creationId)
			if err != mongo.ErrNoDocuments && err != nil {
				return nil, err
			}

			if cardSetDb != nil {
				err = m.CardModel.DeleteByIds(sCtx, cardSetDb.Cards)
				if err != mongo.ErrNoDocuments && err != nil {
					return nil, err
				}
			}

			_, err = m.CardSetCollection.DeleteMany(sCtx, bson.M{"creationId": creationId})
			if err != mongo.ErrNoDocuments {
				return nil, err
			}

			return nil, nil
		},
	)

	if err != nil {
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

func (m *CardSetModel) UpdateCardSet(context context.Context, cardSet *CardSetApi) error {

	cardSetId, err := primitive.ObjectIDFromHex(cardSet.Id)
	if err != nil {
		return err
	}

	userId, err := primitive.ObjectIDFromHex(cardSet.UserId)
	if err != nil {
		return err
	}

	session, err := m.MongoClient.StartSession()
	if err != nil {
		return err
	}

	defer func() {
		session.EndSession(context)
	}()

	_, err = session.WithTransaction(
		context,
		func(sCtx mongo.SessionContext) (interface{}, error) {
			cardSetDb, err := m.LoadCardSetDbById(sCtx, &cardSetId)
			if err != nil {
				return nil, err
			}

			_, err = m.CardModel.SyncCards(sCtx, cardSet.Cards, cardSetDb.Cards, &userId)
			if err != nil {
				return nil, err
			}

			return nil, nil
		},
	)

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
	context context.Context,
	cardSet *CardSetApi,
	userId *primitive.ObjectID,
) (*CardSetApi, error) {

	creationDate, err := time.Parse(time.RFC3339, cardSet.CreationDate)
	if err != nil {
		return nil, err
	}

	var modificationDateTime *primitive.DateTime
	if cardSet.ModificationDate != nil {
		if modificationDate, err := time.Parse(time.RFC3339, *cardSet.ModificationDate); err == nil {
			modificationDateTime = tools.Ptr(primitive.NewDateTimeFromTime(modificationDate))
		}
	}

	session, err := m.MongoClient.StartSession()
	if err != nil {
		return nil, err
	}

	defer func() {
		session.EndSession(context)
	}()

	cardSetDb := &CardSetDb{
		Name:             cardSet.Name,
		UserId:           userId,
		CreationDate:     primitive.NewDateTimeFromTime(creationDate),
		ModificationDate: modificationDateTime,
		CreationId:       cardSet.CreationId,
	}

	_, err = session.WithTransaction(
		context,
		func(sCtx mongo.SessionContext) (interface{}, error) {

			var cardDbIds []*primitive.ObjectID
			for _, crd := range cardSet.Cards {
				cardDb, err := m.CardModel.Insert(sCtx, crd, userId)
				if err != nil {
					return nil, err
				}

				crd.Id = cardDb.ID.Hex()
				cardDbIds = append(cardDbIds, cardDb.ID)
			}

			cardSetDb.Cards = cardDbIds

			res, err := m.CardSetCollection.InsertOne(sCtx, cardSetDb)
			if err != nil {
				return nil, err
			}

			objId := res.InsertedID.(primitive.ObjectID)
			cardSetDb.ID = &objId

			cardSet.Id = objId.Hex()
			cardSet.UserId = userId.Hex()

			return nil, nil
		},
	)

	if err != nil {
		return nil, err
	}

	return cardSet, nil
}
