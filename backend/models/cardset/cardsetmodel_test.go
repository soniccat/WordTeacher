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
	CardSetModel *CardSetModel
	TestMongo    *test.TestMongo
}

func (suite *CardSetTestSuite) SetupTest() {
	//lg := logger.New(true)

	//mongoApp := &TestMongo{
	//	logger: logger.New(true),
	//}
	//err := mongowrapper.SetupMongo(mongoApp, tools.Ptr("mongodb://127.0.0.1:27018/?directConnection=true&replicaSet=rs0"), tools.Ptr(false))
	//if err != nil {
	//	panic(err)
	//}

	suite.TestMongo = test.New()
	//usersDatabase := suite.TestMongo.GetMongoWrapper().Client.Database(mongowrapper.MongoCollectionCardSets)

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
				CreationDate:     "2022-11-03T18:30:02Z",
				ModificationDate: nil,
				CreationId:       "fb23d60c-02f9-4bae-be86-8359274e0e4e",
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
	assert.NotNil(suite.T(), insertedCardSet.Cards[0].Id)

	cardSetId, _ := primitive.ObjectIDFromHex(insertedCardSet.Id)
	loadedCardSetDb, err := suite.CardSetModel.LoadCardSetDbById(ctx, tools.Ptr(cardSetId))
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), insertedCardSet, loadedCardSetDb.ToApi())
}

func TestCardSetTestSuite(t *testing.T) {
	suite.Run(t, new(CardSetTestSuite))
}
