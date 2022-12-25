package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
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

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
)

type BaseTestSuite struct {
	t *testing.T
}

func (b *BaseTestSuite) createUUID() uuid.UUID {
	cardSetCreationIdUUID, err := uuid.NewUUID()
	if err != nil {
		b.t.Fatal(err)
	}
	return cardSetCreationIdUUID
}

type CardSetPushTestSuite struct {
	suite.Suite
	BaseTestSuite
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

func (suite *CardSetPushTestSuite) TestCardSetPush_WithoutAnything_ReturnsBadRequest() {
	req, err := http.NewRequest("POST", "/", nil)
	if err != nil {
		suite.T().Fatal(err)
	}

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)

	assert.Equal(suite.T(), http.StatusInternalServerError, writer.Code)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithCookieButWithoutLastModificationDate_ReturnsBadRequest() {
	req, err := http.NewRequest("POST", "/", nil)
	req.AddCookie(&http.Cookie{Name: "session", Value: "testSession"})
	if err != nil {
		suite.T().Fatal(err)
	}

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)

	assert.Equal(suite.T(), http.StatusInternalServerError, writer.Code)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithInvalidSession_ReturnsUnauthorized() {
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

func (suite *CardSetPushTestSuite) TestCardSetPush_WithNewCardSet_ReturnsOk() {
	cardSetCreationIdUUID := suite.createUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	cardCreationId := suite.createUUID().String()
	newCardSet := createApiCardSet(
		"testCardSet",
		cardSetCreationId,
		time.Now(),
		[]*card.ApiCard{createApiCard(cardCreationId)},
	)

	suite.setupPushValidatorWithCardSet(tools.Ptr(primitive.NewObjectID()), newCardSet)
	req := suite.createPushRequest(time.Now())

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)
	response := suite.readPushResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
	assert.Len(suite.T(), response.CardSetIds, 1)
	assert.Equal(suite.T(), cardSetCreationId, tools.MapKeys(response.CardSetIds)[0])
	assert.NotNil(suite.T(), cardSetCreationId, response.CardSetIds[cardSetCreationId])
	assert.Len(suite.T(), response.CardIds, 1)
	assert.Equal(suite.T(), cardCreationId, tools.MapKeys(response.CardIds)[0])

	dbCardSet := suite.loadCardSetDbById(response.CardSetIds[cardSetCreationId])
	assert.Equal(suite.T(), newCardSet, dbCardSet.ToApi())
	assert.Equal(suite.T(), newCardSet.Id, response.CardSetIds[cardSetCreationId])
	assert.Equal(suite.T(), response.CardIds[cardCreationId], dbCardSet.Cards[0].Id.Hex())
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithAlreadyCardedSet_ReturnsAlreadyCreatedCard() {
	cardSetCreationIdUUID := suite.createUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	newCardSet := createApiCardSet(
		"testCardSet",
		cardSetCreationId,
		time.Now(),
		[]*card.ApiCard{createApiCard(suite.createUUID().String())},
	)

	userId := tools.Ptr(primitive.NewObjectID())
	insertedCardSet, errWithCode := suite.application.cardSetRepository.InsertCardSet(context.Background(), newCardSet, userId)
	if errWithCode != nil {
		suite.T().Fatal(errWithCode.Err)
	}

	newCardSet.Id = "" // clear Id set from InsertCardSet
	suite.setupPushValidatorWithCardSet(userId, newCardSet)
	req := suite.createPushRequest(time.Now())

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)
	response := suite.readPushResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
	assert.Equal(suite.T(), insertedCardSet.Id, response.CardSetIds[cardSetCreationId])

	dbCardSet := suite.loadCardSetDbById(response.CardSetIds[cardSetCreationId])
	assert.Equal(suite.T(), newCardSet, dbCardSet.ToApi())
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithNewCardSetAndOldOne_ReturnsNewCardSetAndDeleteOldOne() {
	oldCardSet := createApiCardSet(
		"oldTestCardSet",
		suite.createUUID().String(),
		time.Now().Add(-time.Hour*time.Duration(10)),
		[]*card.ApiCard{createApiCard(suite.createUUID().String())},
	)

	userId := tools.Ptr(primitive.NewObjectID())
	_, errWithCode := suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet, userId)
	if errWithCode != nil {
		suite.T().Fatal(errWithCode.Err)
	}

	cardSetCreationIdUUID := suite.createUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	newCardSet := createApiCardSet(
		"newTestCardSet",
		cardSetCreationId,
		time.Now(),
		[]*card.ApiCard{createApiCard(suite.createUUID().String())},
	)

	suite.setupPushValidatorWithCardSet(userId, newCardSet)
	req := suite.createPushRequest(time.Now())

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)
	response := suite.readPushResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)

	dbCardSet := suite.loadCardSetDbById(response.CardSetIds[cardSetCreationId])
	assert.Equal(suite.T(), newCardSet, dbCardSet.ToApi())

	_, err := suite.application.cardSetRepository.LoadCardSetDbById(context.Background(), oldCardSet.Id)
	assert.Equal(suite.T(), err, mongo.ErrNoDocuments)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithNotPulledChanges_ReturnsStatusConflict() {
	newCardSet := createApiCardSet(
		"newTestCardSet",
		suite.createUUID().String(),
		time.Now(),
		[]*card.ApiCard{createApiCard(suite.createUUID().String())},
	)

	userId := tools.Ptr(primitive.NewObjectID())
	_, errWithCode := suite.application.cardSetRepository.InsertCardSet(context.Background(), newCardSet, userId)
	if errWithCode != nil {
		suite.T().Fatal(errWithCode.Err)
	}

	cardSetCreationIdUUID := suite.createUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	cardCreationId := suite.createUUID().String()
	oldCardSet := createApiCardSet(
		"oldTestCardSet",
		cardSetCreationId,
		time.Now(),
		[]*card.ApiCard{createApiCard(cardCreationId)},
	)

	suite.setupPushValidatorWithCardSet(userId, oldCardSet)
	req := suite.createPushRequest(time.Now().Add(-time.Hour * time.Duration(20)))

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)

	assert.Equal(suite.T(), http.StatusConflict, writer.Code)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_NewCardSetWithExistingCardSet_ReturnsOk() {
	oldCardSet := createApiCardSet(
		"oldTestCardSet",
		suite.createUUID().String(),
		time.Now().Add(-time.Hour*time.Duration(20)),
		[]*card.ApiCard{createApiCard(suite.createUUID().String())},
	)

	userId := tools.Ptr(primitive.NewObjectID())
	_, errWithCode := suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet, userId)
	if errWithCode != nil {
		suite.T().Fatal(errWithCode.Err)
	}

	cardSetCreationIdUUID := suite.createUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	cardCreationId := suite.createUUID().String()
	newCardSet := createApiCardSet(
		"newTestCardSet",
		cardSetCreationId,
		time.Now(),
		[]*card.ApiCard{createApiCard(cardCreationId)},
	)

	suite.setupPushValidatorWithCardSet(userId, newCardSet)
	req := suite.createPushRequest(time.Now().Add(-time.Hour * time.Duration(10)))

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)
	response := suite.readPushResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
	assert.Len(suite.T(), response.CardSetIds, 1)
	assert.Equal(suite.T(), cardSetCreationId, tools.MapKeys(response.CardSetIds)[0])
	assert.NotEmpty(suite.T(), cardSetCreationId, response.CardSetIds[cardSetCreationId])
	assert.Len(suite.T(), response.CardIds, 1)
	assert.Equal(suite.T(), cardCreationId, tools.MapKeys(response.CardIds)[0])

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
}

// Tools

func (suite *CardSetPushTestSuite) loadCardSetDbById(id string) *cardset.DbCardSet {
	dbCardSet, err := suite.application.cardSetRepository.LoadCardSetDbById(context.Background(), id)
	if err != nil {
		suite.T().Fatal(err)
	}
	return dbCardSet
}

func (suite *CardSetPushTestSuite) createPushRequest(lastModificationDate time.Time) *http.Request {
	req, err := http.NewRequest("POST", fmt.Sprintf("/?%s=%s", ParameterLatestCardSetModificationDate, lastModificationDate.UTC().Format(time.RFC3339)), nil)
	if err != nil {
		suite.T().Fatal(err)
	}
	req.AddCookie(&http.Cookie{Name: "session", Value: "testSession"})
	return req
}

func (suite *CardSetPushTestSuite) setupPushValidatorWithCardSet(userId *primitive.ObjectID, newCardSet *cardset.ApiCardSet) {
	suite.pushValidator.ResponseProvider = func() test.MockSessionValidatorResponse[CardSetPushInput] {
		return test.MockSessionValidatorResponse[CardSetPushInput]{
			&CardSetPushInput{
				"testAccessToken",
				[]*cardset.ApiCardSet{
					newCardSet,
				},
				[]string{},
			},
			createUserAuthToken(userId),
			nil,
		}
	}
}

func (suite *CardSetPushTestSuite) readPushResponse(writer *httptest.ResponseRecorder) *CardSetPushResponse {
	var response CardSetPushResponse
	body, err := io.ReadAll(writer.Result().Body)
	if err != nil {
		suite.T().Fatal(err)
	}

	err = json.Unmarshal(body, &response)
	if err != nil {
		suite.T().Fatal(err)
	}

	return &response
}

func createApiCardSet(name string, creationId string, creationDate time.Time, cards []*card.ApiCard) *cardset.ApiCardSet {
	return &cardset.ApiCardSet{
		Name:             name,
		Cards:            cards,
		CreationDate:     creationDate.UTC().Format(time.RFC3339),
		ModificationDate: tools.Ptr(creationDate.UTC().Format(time.RFC3339)),
		CreationId:       creationId,
	}
}

func createApiCard(creationId string) *card.ApiCard {
	return &card.ApiCard{
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
		CreationId: creationId,
	}
}

func createUserAuthToken(userId *primitive.ObjectID) *userauthtoken.UserAuthToken {
	return &userauthtoken.UserAuthToken{
		Id:          tools.Ptr(primitive.NewObjectID()),
		UserMongoId: userId,
		NetworkType: usernetwork.Google,
		AccessToken: accesstoken.AccessToken{
			Value:          "testAccessToken",
			ExpirationDate: primitive.NewDateTimeFromTime(time.Now()),
		},
		RefreshToken: "testRefreshToken",
		UserDeviceId: "testDeviceId",
	}
}

func TestCardSetPushTestSuite(t *testing.T) {
	suite.Run(t, new(CardSetPushTestSuite))
}
