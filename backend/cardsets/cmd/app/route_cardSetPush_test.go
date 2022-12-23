package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"io"
	"models/accesstoken"
	"models/apphelpers"
	"models/card"
	"models/cardset"
	"models/partofspeech"
	"models/test"
	"models/tools"
	"models/user"
	"models/userauthtoken"
	"models/usernetwork"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

type CardSetPushTestSuite struct {
	suite.Suite
	application   *application
	pushValidator *test.MockSessionValidator[CardSetPushInput]
	pullValidator *test.MockSessionValidator[CardSetPullInput]
}

func (suite *CardSetPushTestSuite) SetupTest() {
	suite.pushValidator = test.NewMockSessionValidator[CardSetPushInput]()
	suite.pullValidator = test.NewMockSessionValidator[CardSetPullInput]()

	sessionManager := apphelpers.CreateSessionManager("172.16.0.3:6380")
	app, err := createApplication(
		true,
		sessionManager,
		"mongodb://127.0.0.1:27018/?directConnection=true&replicaSet=rs0",
		false,
		suite.pushValidator,
		suite.pullValidator,
	)
	if err != nil {
		suite.T().Fatal(err)
	}

	suite.application = app
}

func (suite *CardSetPushTestSuite) TestCardSetPush_ReturnsBadRequest_WithoutAnything() {
	req, err := http.NewRequest("POST", "/", nil)
	if err != nil {
		suite.T().Fatal(err)
	}

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)

	assert.Equal(suite.T(), http.StatusBadRequest, writer.Code)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_ReturnsBadRequest_WithCookieButWithoutLastModificationDate() {
	req, err := http.NewRequest("POST", "/", nil)
	req.AddCookie(&http.Cookie{Name: "session", Value: "testSession"})
	if err != nil {
		suite.T().Fatal(err)
	}

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)

	assert.Equal(suite.T(), http.StatusBadRequest, writer.Code)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_ReturnsUnauthorized_WithInvalidSession() {
	suite.pushValidator.ResponseProvider = func() test.MockSessionValidatorResponse[CardSetPushInput] {
		return test.MockSessionValidatorResponse[CardSetPushInput]{
			nil,
			nil,
			user.NewValidateSessionError(http.StatusUnauthorized, errors.New("test error")),
		}
	}
	req, err := http.NewRequest("POST", fmt.Sprintf("/?%s=2022-11-03T17:30:02Z", ParameterLatestCardSetModificationDate), nil)
	if err != nil {
		suite.T().Fatal(err)
	}
	req.AddCookie(&http.Cookie{Name: "session", Value: "testSession"})

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)

	assert.Equal(suite.T(), http.StatusUnauthorized, writer.Code)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_ReturnsOk_WithNewCardSet() {
	cardSetCreationIdUUID, err := uuid.NewUUID()
	if err != nil {
		suite.T().Fatal(err)
	}
	cardSetCreationId := cardSetCreationIdUUID.String()

	newCardSet := &cardset.ApiCardSet{
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
		CreationId:       cardSetCreationId,
	}

	suite.pushValidator.ResponseProvider = func() test.MockSessionValidatorResponse[CardSetPushInput] {
		return test.MockSessionValidatorResponse[CardSetPushInput]{
			&CardSetPushInput{
				"testAccessToken",
				[]*cardset.ApiCardSet{
					newCardSet,
				},
				[]string{},
			},
			&userauthtoken.UserAuthToken{
				Id:          tools.Ptr(primitive.NewObjectID()),
				UserMongoId: tools.Ptr(primitive.NewObjectID()),
				NetworkType: usernetwork.Google,
				AccessToken: accesstoken.AccessToken{
					Value:          "testAccessToken",
					ExpirationDate: primitive.NewDateTimeFromTime(time.Now()),
				},
				RefreshToken: "testRefreshToken",
				UserDeviceId: "testDeviceId",
			},
			nil,
		}
	}
	req, err := http.NewRequest("POST", fmt.Sprintf("/?%s=2022-11-03T17:30:02Z", ParameterLatestCardSetModificationDate), nil)
	if err != nil {
		suite.T().Fatal(err)
	}
	req.AddCookie(&http.Cookie{Name: "session", Value: "testSession"})

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)

	var response CardSetPushResponse
	body, err := io.ReadAll(writer.Result().Body)
	if err != nil {
		suite.T().Fatal(err)
	}

	err = json.Unmarshal(body, &response)
	if err != nil {
		suite.T().Fatal(err)
	}

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
	assert.Len(suite.T(), response.CardSetIds, 1)
	assert.Equal(suite.T(), cardSetCreationId, tools.MapKeys(response.CardSetIds)[0])
	assert.NotNil(suite.T(), cardSetCreationId, response.CardSetIds[cardSetCreationId])

	dbCardSet, err := suite.application.cardSetRepository.LoadCardSetDbById(context.Background(), response.CardSetIds[cardSetCreationId])
	if err != nil {
		suite.T().Fatal(err)
	}

	assert.Equal(suite.T(), newCardSet, dbCardSet.ToApi())
}

func TestUserModelTestSuite(t *testing.T) {
	suite.Run(t, new(CardSetPushTestSuite))
}
