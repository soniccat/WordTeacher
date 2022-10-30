package card

import (
	"context"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"models/logger"
	"models/tools"
	"time"
)

type CardModel struct {
	logger         *logger.Logger
	cardCollection *mongo.Collection
}

func (cm *CardModel) Insert(
	context context.Context,
	card *CardApi,
	userId primitive.ObjectID,
) (*CardDb, error) {
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

	res, err := cm.cardCollection.InsertOne(context, cardDb)
	if err != nil {
		return nil, err
	}

	resId := res.InsertedID.(primitive.ObjectID)
	card.Id = resId.String()
	cardDb.ID = resId

	return cardDb, nil
}
