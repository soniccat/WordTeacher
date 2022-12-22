package cardset

import (
	"context"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/card"
	"models/partofspeech"
	"models/test"
	"models/tools"
	"testing"
)

type CardSetTestSuite struct {
	suite.Suite
	CardSetModel *Repository
	TestMongo    *test.TestMongo
}

func (suite *CardSetTestSuite) SetupTest() {
	suite.TestMongo = test.New()
	suite.CardSetModel = New(
		suite.TestMongo.GetLogger(),
		suite.TestMongo.GetMongoWrapper().Client,
	)
}

func (suite *CardSetTestSuite) TestCreateCardSet() {
	ctx := context.Background()
	cardSet := &CardSetApi{
		Name: "testCardSet",
		Cards: []*card.CardApi{
			{
				Term:          "testTerm1",
				Transcription: tools.Ptr("testTranscription"),
				PartOfSpeech:  partofspeech.Adverb,
				Definitions:   []string{"testDef1", "testDef2"},
				Synonyms:      []string{"testSyn1", "testSyn2"},
				Examples:      []string{"testEx1", "testEx2"},
				DefinitionTermSpans: [][]card.Span{
					[]card.Span{{1, 2}, {3, 4}},
					[]card.Span{{5, 6}, {7, 8}},
				},
				ExampleTermSpans: [][]card.Span{
					[]card.Span{{9, 10}, {11, 12}},
					[]card.Span{{13, 14}, {15, 16}},
				},
				CreationId: "fb23d60c-02f9-4bae-be86-8359274e0e4e",
			},
		},
		CreationDate:     "2022-11-03T17:30:02Z",
		ModificationDate: nil,
		CreationId:       "bf3d4938-3568-4da7-81ad-a2342745adee",
	}
	ownerId := primitive.NewObjectID()

	insertedCardSet, errWithCode := suite.CardSetModel.InsertCardSet(ctx, cardSet, &ownerId)
	assert.Nil(suite.T(), errWithCode)
	assert.NotNil(suite.T(), insertedCardSet.Id)
	assert.Equal(suite.T(), ownerId.Hex(), insertedCardSet.UserId)
	assert.NotNil(suite.T(), insertedCardSet.Cards[0].Id)
	assert.Equal(suite.T(), ownerId.Hex(), insertedCardSet.Cards[0].UserId)

	cardSetId, _ := primitive.ObjectIDFromHex(insertedCardSet.Id)
	loadedCardSetDb, err := suite.CardSetModel.LoadCardSetDbById(ctx, tools.Ptr(cardSetId))
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), insertedCardSet, loadedCardSetDb.ToApi())
}

func (suite *CardSetTestSuite) TestUpdateCardSetWithNewCard() {
	ctx := context.Background()
	cardSet := &CardSetApi{
		Name: "testCardSet",
		Cards: []*card.CardApi{
			{
				Term:          "testTerm1",
				Transcription: tools.Ptr("testTranscription"),
				PartOfSpeech:  partofspeech.Adverb,
				Definitions:   []string{"testDef1", "testDef2"},
				Synonyms:      []string{"testSyn1", "testSyn2"},
				Examples:      []string{"testEx1", "testEx2"},
				DefinitionTermSpans: [][]card.Span{
					[]card.Span{{1, 2}, {3, 4}},
					[]card.Span{{5, 6}, {7, 8}},
				},
				ExampleTermSpans: [][]card.Span{
					[]card.Span{{9, 10}, {11, 12}},
					[]card.Span{{13, 14}, {15, 16}},
				},
				CreationId: "fb23d60c-02f9-4bae-be86-8359274e0e4e",
			},
		},
		CreationDate:     "2022-11-03T17:30:02Z",
		ModificationDate: nil,
		CreationId:       "bf3d4938-3568-4da7-81ad-a2342745adee",
	}
	ownerId := primitive.NewObjectID()

	insertedCardSet, errWithCode := suite.CardSetModel.InsertCardSet(ctx, cardSet, &ownerId)
	assert.Nil(suite.T(), errWithCode)

	insertedCardSet.Cards = append(
		insertedCardSet.Cards,
		&card.CardApi{
			Term:          "testTerm2",
			Transcription: tools.Ptr("testTranscription2"),
			PartOfSpeech:  partofspeech.Adverb,
			Definitions:   []string{"testDef3", "testDef4"},
			Synonyms:      []string{"testSyn3", "testSyn4"},
			Examples:      []string{"testEx3", "testEx4"},
			DefinitionTermSpans: [][]card.Span{
				[]card.Span{{10, 20}, {3, 4}},
				[]card.Span{{50, 60}, {7, 8}},
			},
			ExampleTermSpans: [][]card.Span{
				[]card.Span{{90, 100}, {11, 12}},
				[]card.Span{{130, 140}, {15, 16}},
			},
			CreationId: "1aed98e4-3ec7-403b-8e5e-3d2ca997e5d5",
		},
	)

	errWithCode = suite.CardSetModel.UpdateCardSet(ctx, insertedCardSet)
	assert.Nil(suite.T(), errWithCode)

	cardSetId, _ := primitive.ObjectIDFromHex(insertedCardSet.Id)
	loadedCardSetDb, err := suite.CardSetModel.LoadCardSetDbById(ctx, tools.Ptr(cardSetId))
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), insertedCardSet, loadedCardSetDb.ToApi())
}

func TestCardSetTestSuite(t *testing.T) {
	suite.Run(t, new(CardSetTestSuite))
}
