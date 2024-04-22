package storage

import (
	"api"
	"context"
	"log/slog"
	"os"
	"testing"
	"tools"
	"tools/logger"
	"tools/test"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

type CardSetTestSuite struct {
	suite.Suite
	CardSetModel *Storage
}

func (suite *CardSetTestSuite) SetupTest() {
	var err error
	logger := logger.New(os.Stdout, slog.LevelDebug)
	suite.CardSetModel, err = New(
		logger,
		test.ConnectionString,
		false,
	)

	if err != nil {
		suite.T().Fatal(err)
	}
}

func (suite *CardSetTestSuite) TestCreateCardSet() {
	ctx := context.Background()
	cardSet := &api.CardSet{
		Name: "testCardSet",
		Cards: []*api.Card{
			{
				Term:          "testTerm1",
				Transcription: tools.Ptr("testTranscription"),
				PartOfSpeech:  api.Adverb,
				Definitions:   []string{"testDef1", "testDef2"},
				Synonyms:      []string{"testSyn1", "testSyn2"},
				Examples:      []string{"testEx1", "testEx2"},
				DefinitionTermSpans: [][]api.Span{
					{{1, 2}, {3, 4}},
					{{5, 6}, {7, 8}},
				},
				ExampleTermSpans: [][]api.Span{
					{{9, 10}, {11, 12}},
					{{13, 14}, {15, 16}},
				},
				CreationId: "fb23d60c-02f9-4bae-be86-8359274e0e4e",
			},
		},
		CreationDate:     "2022-11-03T17:30:02Z",
		ModificationDate: "2022-11-03T17:30:02Z",
		CreationId:       "bf3d4938-3568-4da7-81ad-a2342745adee",
	}
	ownerId := primitive.NewObjectID().Hex()

	insertedCardSet, errWithCode := suite.CardSetModel.InsertCardSet(ctx, cardSet, ownerId)
	assert.Nil(suite.T(), errWithCode)
	assert.NotNil(suite.T(), insertedCardSet.Id)
	assert.Equal(suite.T(), ownerId, insertedCardSet.UserId)
	assert.NotNil(suite.T(), insertedCardSet.Cards[0].Id)
	assert.Equal(suite.T(), ownerId, insertedCardSet.Cards[0].UserId)

	loadedCardSetDb, err := suite.CardSetModel.LoadCardSetDbById(ctx, insertedCardSet.Id)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), insertedCardSet, loadedCardSetDb.ToApi())
}

func (suite *CardSetTestSuite) TestUpdateCardSetWithNewCard() {
	ctx := context.Background()
	cardSet := &api.CardSet{
		Name: "testCardSet",
		Cards: []*api.Card{
			{
				Term:          "testTerm1",
				Transcription: tools.Ptr("testTranscription"),
				PartOfSpeech:  api.Adverb,
				Definitions:   []string{"testDef1", "testDef2"},
				Synonyms:      []string{"testSyn1", "testSyn2"},
				Examples:      []string{"testEx1", "testEx2"},
				DefinitionTermSpans: [][]api.Span{
					{{1, 2}, {3, 4}},
					{{5, 6}, {7, 8}},
				},
				ExampleTermSpans: [][]api.Span{
					{{9, 10}, {11, 12}},
					{{13, 14}, {15, 16}},
				},
				CreationId: "fb23d60c-02f9-4bae-be86-8359274e0e4e",
			},
		},
		CreationDate:     "2022-11-03T17:30:02Z",
		ModificationDate: "2022-11-03T17:30:02Z",
		CreationId:       "bf3d4938-3568-4da7-81ad-a2342745adee",
	}
	ownerId := primitive.NewObjectID().Hex()

	insertedCardSet, errWithCode := suite.CardSetModel.InsertCardSet(ctx, cardSet, ownerId)
	assert.Nil(suite.T(), errWithCode)

	insertedCardSet.Cards = append(
		insertedCardSet.Cards,
		&api.Card{
			Term:          "testTerm2",
			Transcription: tools.Ptr("testTranscription2"),
			PartOfSpeech:  api.Adverb,
			Definitions:   []string{"testDef3", "testDef4"},
			Synonyms:      []string{"testSyn3", "testSyn4"},
			Examples:      []string{"testEx3", "testEx4"},
			DefinitionTermSpans: [][]api.Span{
				{{10, 20}, {3, 4}},
				{{50, 60}, {7, 8}},
			},
			ExampleTermSpans: [][]api.Span{
				{{90, 100}, {11, 12}},
				{{130, 140}, {15, 16}},
			},
			CreationId: "1aed98e4-3ec7-403b-8e5e-3d2ca997e5d5",
		},
	)

	errWithCode = suite.CardSetModel.UpdateCardSet(ctx, insertedCardSet)
	assert.Nil(suite.T(), errWithCode)

	loadedCardSetDb, err := suite.CardSetModel.LoadCardSetDbById(ctx, insertedCardSet.Id)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), insertedCardSet, loadedCardSetDb.ToApi())
}

func TestCardSetTestSuite(t *testing.T) {
	suite.Run(t, new(CardSetTestSuite))
}
