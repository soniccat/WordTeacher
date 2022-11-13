package card

import (
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"models/logger"
	"models/mongowrapper"
	"models/tools"
	"time"
)

type CardModel struct {
	Logger         *logger.Logger
	CardCollection *mongo.Collection
}

func New(logger *logger.Logger, cardSetDatabase *mongo.Database) *CardModel {
	model := &CardModel{
		Logger:         logger,
		CardCollection: cardSetDatabase.Collection(mongowrapper.MongoCollectionCards),
	}

	return model
}

func (cm *CardModel) SyncCards(
	ctx context.Context,
	actualCards []*CardApi,
	currentCardIds []primitive.ObjectID,
	userId primitive.ObjectID,
) ([]*CardApi, error) {

	var newCards []*CardApi
	var deletedCards []primitive.ObjectID
	var updatedCards []*CardApi
	var actualCardsWithIds []*CardApi

	actualCardIds, err := tools.MapNotNilOrError(actualCards, func(c **CardApi) (*primitive.ObjectID, error) {
		if len((*c).Id) == 0 {
			newCards = append(newCards, *c)
			return nil, nil
		}

		cardId, fErr := primitive.ObjectIDFromHex((*c).Id)
		if fErr != nil {
			return nil, fErr
		}

		actualCardsWithIds = append(actualCardsWithIds, *c)
		return &cardId, nil
	})
	if err != nil {
		return nil, err
	}

	var resultCards []*CardApi

	dbCards, err := cm.LoadByIds(ctx, currentCardIds)
	if err != nil {
		return nil, err
	}

	for i, actualCardId := range actualCardIds {
		if tools.FindOrNil[CardDb](dbCards, func(c *CardDb) bool { return c.ID == *actualCardId }) == nil {
			newCards = append(newCards, actualCardsWithIds[i])
		} else {
			updatedCards = append(updatedCards, actualCardsWithIds[i])
		}
	}

	for _, cardDb := range dbCards {
		if tools.FindOrNil[primitive.ObjectID](actualCardIds, func(id *primitive.ObjectID) bool { return *id == cardDb.ID }) == nil {
			deletedCards = append(deletedCards, cardDb.ID)
		}
	}

	// perform
	err = cm.DeleteByIds(ctx, deletedCards)
	if err != nil {
		return nil, err
	}

	insertedDbCards, err := cm.InsertCards(ctx, newCards, userId)
	if err != nil {
		return nil, err
	}

	updatedDbCards, err := cm.ReplaceCards(ctx, updatedCards)

	return resultCards, nil
}

func (cm *CardModel) LoadByIds(context context.Context, ids []primitive.ObjectID) ([]*CardDb, error) {
	var result []*CardDb
	cursor, err := cm.CardCollection.Find(context, bson.M{"_id": bson.M{"$in": ids}})
	if err != nil {
		return nil, err
	}

	err = cursor.All(context, result)
	if err != nil {
		return nil, err
	}

	return result, nil
}

func (cm *CardModel) DeleteByIds(context context.Context, ids []primitive.ObjectID) error {
	_, err := cm.CardCollection.DeleteMany(context, bson.M{"_id": bson.M{"$in": ids}})
	return err
}

func (cm *CardModel) ReplaceCards(
	context context.Context,
	cards []*CardApi,
) ([]*CardDb, error) {
	var cardDbs []*CardDb

	for _, card := range cards {
		cardId, err := primitive.ObjectIDFromHex((*card).Id)
		if err != nil {
			return nil, err
		}

		cardDb, err := card.ToDb()
		if err != nil {
			return nil, err
		}

		_, err = cm.CardCollection.ReplaceOne(context, bson.M{"_id": cardId}, cardDb)
		if err != nil {
			return nil, err
		}

		cardDbs = append(cardDbs, cardDb)
	}

	return cardDbs, nil
}

func (cm *CardModel) Insert(
	context context.Context,
	card *CardApi,
	userId primitive.ObjectID,
) (*CardDb, error) {
	cardDb, err := cm.createCardDb(card, userId)
	if err != nil {
		return nil, err
	}

	res, err := cm.CardCollection.InsertOne(context, cardDb)
	if err != nil {
		return nil, err
	}

	resId := res.InsertedID.(primitive.ObjectID)
	card.Id = resId.String()
	cardDb.ID = resId

	return cardDb, nil
}

func (cm *CardModel) InsertCards(
	context context.Context,
	cards []*CardApi,
	userId primitive.ObjectID,
) ([]*CardDb, error) {
	var cardDbs []*CardDb
	cardDbs, err := tools.MapOrError(cards, func(c **CardApi) (*CardDb, error) {
		cardDb, e := cm.createCardDb(*c, userId)
		return cardDb, e
	})
	if err != nil {
		return nil, err
	}

	cardDbInterfaces := tools.Map(cardDbs, func(c *CardDb) interface{} { return c })
	manyResult, err := cm.CardCollection.InsertMany(context, cardDbInterfaces)
	if err != nil {
		return nil, err
	}

	for i, insertedId := range manyResult.InsertedIDs {
		resId := insertedId.(primitive.ObjectID)
		cards[i].Id = resId.String()
		cardDbs[i].ID = resId
	}

	return cardDbs, nil
}

func (cm *CardModel) createCardDb(card *CardApi, userId primitive.ObjectID) (*CardDb, error) {
	creationDate, err := time.Parse(time.RFC3339, card.CreationDate)
	if err != nil {
		return nil, err
	}

	var modificationDateTime *primitive.DateTime
	if card.ModificationDate != nil {
		if modificationDate, err := time.Parse(time.RFC3339, *card.ModificationDate); err == nil {
			modificationDateTime = tools.Ptr(primitive.NewDateTimeFromTime(modificationDate))
		}
	}

	cardDb := &CardDb{
		Term:                card.Term,
		Transcription:       card.Transcription,
		PartOfSpeech:        card.PartOfSpeech,
		Definitions:         card.Definitions,
		Synonyms:            card.Synonyms,
		Examples:            card.Examples,
		DefinitionTermSpans: card.DefinitionTermSpans,
		ExampleTermSpans:    card.ExampleTermSpans,
		UserId:              userId,
		CreationDate:        primitive.NewDateTimeFromTime(creationDate),
		ModificationDate:    modificationDateTime,
	}
	return cardDb, nil
}
