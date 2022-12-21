package card

import (
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"models/logger"
	"models/mongowrapper"
	"models/tools"
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
	ctx context.Context, // transaction is required
	actualCards []*CardApi,
	currentCardIds []*primitive.ObjectID,
	userId *primitive.ObjectID,
) ([]*CardApi, error) {

	// initialize empty slices on purpose as mongo Api will crash
	newCards := []*CardApi{}
	deletedCards := []*primitive.ObjectID{}
	updatedCards := []*CardApi{}
	actualCardsWithIds := []*CardApi{}

	actualCardIds, err := tools.MapNotNilOrError(actualCards, func(c *CardApi) (*primitive.ObjectID, error) {
		if len((*c).Id) == 0 {
			newCards = append(newCards, c)
			return nil, nil
		}

		cardId, fErr := primitive.ObjectIDFromHex((*c).Id)
		if fErr != nil {
			return nil, fErr
		}

		actualCardsWithIds = append(actualCardsWithIds, c)
		return &cardId, nil
	})
	if err != nil {
		return nil, err
	}

	dbCards, err := cm.LoadByIds(ctx, currentCardIds)
	if err != nil {
		return nil, err
	}

	for i, actualCardId := range actualCardIds {
		if tools.FindOrNil(dbCards, func(c *CardDb) bool { return *c.Id == *actualCardId }) == nil {
			newCards = append(newCards, actualCardsWithIds[i])
		} else {
			updatedCards = append(updatedCards, actualCardsWithIds[i])
		}
	}

	for _, dbCard := range dbCards {
		if tools.FindOrNil(actualCardIds, func(id *primitive.ObjectID) bool { return *id == *dbCard.Id }) == nil {
			deletedCards = append(deletedCards, dbCard.Id)
		}
	}

	// perform
	err = cm.DeleteByIds(ctx, deletedCards)
	if err != nil {
		return nil, err
	}

	dbCards, err = cm.InsertCards(ctx, newCards, userId)
	if err != nil {
		return nil, err
	}

	for i, dbCard := range dbCards {
		newCards[i].Id = dbCard.Id.Hex()
	}

	_, err = cm.ReplaceCards(ctx, updatedCards)
	if err != nil {
		return nil, err
	}

	return tools.SliceAppend(newCards, updatedCards), nil
}

func (cm *CardModel) LoadByIds(context context.Context, ids []*primitive.ObjectID) ([]*CardDb, error) {
	var result []*CardDb
	cursor, err := cm.CardCollection.Find(context, bson.M{"_id": bson.M{"$in": ids}})
	if err != nil {
		return nil, err
	}

	err = cursor.All(context, &result)
	if err != nil {
		return nil, err
	}

	return result, nil
}

func (cm *CardModel) DeleteByIds(context context.Context, ids []*primitive.ObjectID) error {
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

//func (cm *CardModel) Insert(
//	context context.Context,
//	card *CardApi,
//	userId *primitive.ObjectID,
//) (*CardDb, error) {
//	cardDb, err := cm.createCardDb(card, userId)
//	if err != nil {
//		return nil, err
//	}
//
//	res, err := cm.CardCollection.InsertOne(context, cardDb)
//	if err != nil {
//		return nil, err
//	}
//
//	resId := res.InsertedID.(primitive.ObjectID)
//	card.Id = resId.Hex()
//	cardDb.Id = &resId
//
//	return cardDb, nil
//}

func (cm *CardModel) InsertCards(
	context context.Context,
	cards []*CardApi,
	userId *primitive.ObjectID,
) ([]*CardDb, error) {
	var cardDbs []*CardDb
	cardDbs, err := tools.MapOrError(cards, func(c *CardApi) (*CardDb, error) {
		cardDb, e := c.ToDb()
		if cardDb != nil {
			cardDb.UserId = userId
		}
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
		cards[i].Id = resId.Hex()
		cardDbs[i].Id = &resId
	}

	return cardDbs, nil
}

//func (cm *CardModel) createCardDb(card *CardApi, userId *primitive.ObjectID) (*CardDb, error) {
//	cardDb := &CardDb{
//		Term:                card.Term,
//		Transcription:       card.Transcription,
//		PartOfSpeech:        card.PartOfSpeech,
//		Definitions:         card.Definitions,
//		Synonyms:            card.Synonyms,
//		Examples:            card.Examples,
//		DefinitionTermSpans: card.DefinitionTermSpans,
//		ExampleTermSpans:    card.ExampleTermSpans,
//		UserId:              userId,
//		CreationId:          card.CreationId,
//	}
//	return cardDb, nil
//}
