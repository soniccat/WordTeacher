package cardset

import (
	"context"
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

func (m *CardSetModel) InsertCardSet(
	context context.Context,
	cardSet *CardSetApi,
	userId primitive.ObjectID,
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
	}

	_, err = session.WithTransaction(
		context,
		func(sCtx mongo.SessionContext) (interface{}, error) {

			cardDbIds := make([]primitive.ObjectID, len(cardSet.Cards))
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
			cardSetDb.ID = objId

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
