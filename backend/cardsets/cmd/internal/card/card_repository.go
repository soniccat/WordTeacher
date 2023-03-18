package card

import (
	"api"
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"models/logger"
	"models/mongowrapper"
	"tools"
)

type Repository struct {
	Logger         *logger.Logger
	CardCollection *mongo.Collection
}

func New(logger *logger.Logger, cardSetDatabase *mongo.Database) *Repository {
	model := &Repository{
		Logger:         logger,
		CardCollection: cardSetDatabase.Collection(mongowrapper.MongoCollectionCards),
	}

	return model
}

func (cm *Repository) SyncCards(
	ctx context.Context, // transaction is required
	actualCards []*api.ApiCard,
	currentCardIds []*primitive.ObjectID,
	userId *primitive.ObjectID,
) ([]*api.ApiCard, error) {

	// initialize empty slices on purpose as mongo Api will crash on nils
	newCards := []*api.ApiCard{}
	deletedCards := []*primitive.ObjectID{}
	updatedCards := []*api.ApiCard{}
	actualCardsWithIds := []*api.ApiCard{}

	actualCardIds, err := tools.MapNotNilOrError(actualCards, func(c *api.ApiCard) (*primitive.ObjectID, error) {
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
		if tools.FindOrNil(dbCards, func(c *DbCard) bool { return *c.Id == *actualCardId }) == nil {
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

func (cm *Repository) LoadByIds(context context.Context, ids []*primitive.ObjectID) ([]*DbCard, error) {
	var result []*DbCard
	cursor, err := cm.CardCollection.Find(context, bson.M{"_id": bson.M{"$in": ids}})
	if err != nil {
		return nil, err
	}

	defer func() { cursor.Close(context) }()

	err = cursor.All(context, &result)
	if err != nil {
		return nil, err
	}

	return result, nil
}

func (cm *Repository) DeleteByIds(context context.Context, ids []*primitive.ObjectID) error {
	_, err := cm.CardCollection.DeleteMany(context, bson.M{"_id": bson.M{"$in": ids}})
	return err
}

func (cm *Repository) ReplaceCards(
	context context.Context,
	cards []*api.ApiCard,
) ([]*DbCard, error) {
	var cardDbs []*DbCard

	for _, card := range cards {
		cardId, err := primitive.ObjectIDFromHex((*card).Id)
		if err != nil {
			return nil, err
		}

		cardDb, err := ApiCardToDb(card)
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

//func (cm *Repository) Insert(
//	context context.Context,
//	card *ApiCard,
//	userId *primitive.ObjectID,
//) (*DbCard, error) {
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

func (cm *Repository) InsertCards(
	context context.Context,
	cards []*api.ApiCard,
	userId *primitive.ObjectID,
) ([]*DbCard, error) {
	var cardDbs []*DbCard
	cardDbs, err := tools.MapOrError(cards, func(c *api.ApiCard) (*DbCard, error) {
		cardDb, e := ApiCardToDb(c)
		if cardDb != nil {
			cardDb.UserId = userId
		}
		return cardDb, e
	})
	if err != nil {
		return nil, err
	}

	cardDbInterfaces := tools.Map(cardDbs, func(c *DbCard) interface{} { return c })
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

//func (cm *Repository) createCardDb(card *ApiCard, userId *primitive.ObjectID) (*DbCard, error) {
//	cardDb := &DbCard{
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
