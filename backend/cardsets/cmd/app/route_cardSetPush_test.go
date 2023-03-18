package main

import (
	"api"
	"bytes"
	"cardsets/cmd/internal/cardset"
	"context"
	"errors"
	"fmt"
	"models"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
	"tools"
	"tools/apphelpers"
	"tools/test"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
)

type CardSetPushTestSuite struct {
	suite.Suite
	test.BaseTestSuite
	application      *application
	sessionValidator *models.MockSessionValidator
}

func (suite *CardSetPushTestSuite) SetupTest() {
	suite.sessionValidator = models.NewMockSessionValidator()

	sessionManager := apphelpers.CreateSessionManager("172.16.0.3:6380")
	app, err := createApplication(
		true,
		sessionManager,
		"mongodb://127.0.0.1:27018/?directConnection=true&replicaSet=rs0",
		false,
		suite.sessionValidator,
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
	suite.sessionValidator.ResponseProvider = func() models.MockSessionValidatorResponse {
		return models.MockSessionValidatorResponse{
			nil,
			models.NewValidateSessionError(http.StatusUnauthorized, errors.New("test error")),
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
	cardSetCreationIdUUID := suite.CreateUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	cardCreationId := suite.CreateUUID().String()
	newCardSet := createApiCardSet(
		"testCardSet",
		cardSetCreationId,
		time.Now(),
		[]*api.ApiCard{createApiCard(cardCreationId)},
	)

	suite.setupPushValidator(tools.Ptr(primitive.NewObjectID()))
	req := suite.createPushRequest(
		tools.Ptr(time.Now()),
		CardSetPushInput{[]*api.ApiCardSet{newCardSet}, []string{}},
	)

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
	assert.NotNil(suite.T(), dbCardSet.UserId)
	assert.Equal(suite.T(), newCardSet, dbCardSet.ToApi().WithoutIDs())
	assert.Equal(suite.T(), response.CardSetIds[cardSetCreationId], dbCardSet.Id.Hex())
	assert.Equal(suite.T(), response.CardIds[cardCreationId], dbCardSet.Cards[0].Id.Hex())
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithAlreadyCardedSet_ReturnsAlreadyCreatedCard() {
	cardSetCreationIdUUID := suite.CreateUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	apiCardCreationId := suite.CreateUUID().String()
	modificationDate := time.Now()
	newCardSet := createApiCardSet(
		"testCardSet",
		cardSetCreationId,
		modificationDate,
		[]*api.ApiCard{createApiCard(apiCardCreationId)},
	)

	userId := tools.Ptr(primitive.NewObjectID())
	insertedCardSet, errWithCode := suite.application.cardSetRepository.InsertCardSet(context.Background(), newCardSet, userId)
	if errWithCode != nil {
		suite.T().Fatal(errWithCode.Err)
	}

	newCardSet = createApiCardSet(
		"testCardSet",
		cardSetCreationId,
		modificationDate,
		[]*api.ApiCard{createApiCard(apiCardCreationId)},
	)
	suite.setupPushValidator(userId)
	req := suite.createPushRequest(
		tools.Ptr(time.Now()),
		CardSetPushInput{[]*api.ApiCardSet{newCardSet}, []string{}},
	)

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)
	response := suite.readPushResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
	assert.Equal(suite.T(), insertedCardSet.Id, response.CardSetIds[cardSetCreationId])

	dbCardSet := suite.loadCardSetDbById(response.CardSetIds[cardSetCreationId])
	assert.NotNil(suite.T(), dbCardSet.UserId)
	assert.Equal(suite.T(), newCardSet, dbCardSet.ToApi().WithoutIDs())
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithNewCardSetAndOldOne_ReturnsNewCardSetAndDeleteOldOne() {
	oldCardSet := createApiCardSet(
		"oldTestCardSet",
		suite.CreateUUID().String(),
		time.Now().Add(-time.Hour*time.Duration(10)),
		[]*api.ApiCard{createApiCard(suite.CreateUUID().String())},
	)

	userId := tools.Ptr(primitive.NewObjectID())
	_, errWithCode := suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet, userId)
	if errWithCode != nil {
		suite.T().Fatal(errWithCode.Err)
	}

	cardSetCreationIdUUID := suite.CreateUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	newCardSet := createApiCardSet(
		"newTestCardSet",
		cardSetCreationId,
		time.Now(),
		[]*api.ApiCard{createApiCard(suite.CreateUUID().String())},
	)

	suite.setupPushValidator(userId)
	req := suite.createPushRequest(
		tools.Ptr(time.Now()),
		CardSetPushInput{[]*api.ApiCardSet{newCardSet}, []string{}},
	)

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)
	response := suite.readPushResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)

	dbCardSet := suite.loadCardSetDbById(response.CardSetIds[cardSetCreationId])
	assert.Equal(suite.T(), newCardSet, dbCardSet.ToApi().WithoutIDs())

	_, err := suite.application.cardSetRepository.LoadCardSetDbById(context.Background(), oldCardSet.Id)
	assert.Equal(suite.T(), err, mongo.ErrNoDocuments)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithNotPulledChanges_ReturnsStatusConflict() {
	newCardSet := createApiCardSet(
		"newTestCardSet",
		suite.CreateUUID().String(),
		time.Now(),
		[]*api.ApiCard{createApiCard(suite.CreateUUID().String())},
	)

	userId := tools.Ptr(primitive.NewObjectID())
	_, errWithCode := suite.application.cardSetRepository.InsertCardSet(context.Background(), newCardSet, userId)
	if errWithCode != nil {
		suite.T().Fatal(errWithCode.Err)
	}

	cardSetCreationIdUUID := suite.CreateUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	cardCreationId := suite.CreateUUID().String()
	oldCardSet := createApiCardSet(
		"oldTestCardSet",
		cardSetCreationId,
		time.Now(),
		[]*api.ApiCard{createApiCard(cardCreationId)},
	)

	suite.setupPushValidator(userId)
	req := suite.createPushRequest(
		tools.Ptr(time.Now().Add(-time.Hour*time.Duration(20))),
		CardSetPushInput{[]*api.ApiCardSet{oldCardSet}, []string{}},
	)

	writer := httptest.NewRecorder()
	suite.application.cardSetPush(writer, req)

	assert.Equal(suite.T(), http.StatusConflict, writer.Code)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_NewCardSetWithExistingCardSet_ReturnsOk() {
	oldCardSet := createApiCardSet(
		"oldTestCardSet",
		suite.CreateUUID().String(),
		time.Now().Add(-time.Hour*time.Duration(20)),
		[]*api.ApiCard{createApiCard(suite.CreateUUID().String())},
	)

	userId := tools.Ptr(primitive.NewObjectID())
	_, errWithCode := suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet, userId)
	if errWithCode != nil {
		suite.T().Fatal(errWithCode.Err)
	}

	cardSetCreationIdUUID := suite.CreateUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	cardCreationId := suite.CreateUUID().String()
	newCardSet := createApiCardSet(
		"newTestCardSet",
		cardSetCreationId,
		time.Now(),
		[]*api.ApiCard{createApiCard(cardCreationId)},
	)

	suite.setupPushValidator(userId)
	req := suite.createPushRequest(
		tools.Ptr(time.Now().Add(-time.Hour*time.Duration(10))),
		CardSetPushInput{[]*api.ApiCardSet{newCardSet}, []string{}},
	)

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

func (suite *CardSetPushTestSuite) createPushRequest(lastModificationDate *time.Time, input CardSetPushInput) *http.Request {
	var resultPath = ""
	if lastModificationDate != nil {
		resultPath = fmt.Sprintf("/?%s=%s", ParameterLatestCardSetModificationDate, lastModificationDate.UTC().Format(time.RFC3339))
	}

	req, err := http.NewRequest("POST", resultPath, bytes.NewReader(test.TestMarshal(input)))
	if err != nil {
		suite.T().Fatal(err)
	}
	req.AddCookie(&http.Cookie{Name: "session", Value: "testSession"})
	return req
}

func (suite *CardSetPushTestSuite) setupPushValidator(userId *primitive.ObjectID) {
	suite.sessionValidator.ResponseProvider = func() models.MockSessionValidatorResponse {
		return models.MockSessionValidatorResponse{
			createUserAuthToken(userId),
			nil,
		}
	}
}

func (suite *CardSetPushTestSuite) readPushResponse(writer *httptest.ResponseRecorder) *CardSetPushResponse {
	return test.TestReadResponse[CardSetPushResponse](writer)
}

func createApiCardSet(name string, creationId string, creationDate time.Time, cards []*api.ApiCard) *api.ApiCardSet {
	return &api.ApiCardSet{
		Name:             name,
		Cards:            cards,
		CreationDate:     creationDate.UTC().Format(time.RFC3339),
		ModificationDate: creationDate.UTC().Format(time.RFC3339),
		CreationId:       creationId,
	}
}

func createApiCard(creationId string) *api.ApiCard {
	return &api.ApiCard{
		Term:          "testTerm1",
		Transcription: tools.Ptr("testTranscription"),
		PartOfSpeech:  api.Adverb,
		Definitions:   []string{"testDef1", "testDef2"},
		Synonyms:      []string{"testSyn1", "testSyn2"},
		Examples:      []string{"testEx1", "testEx2"},
		DefinitionTermSpans: [][]api.Span{
			[]api.Span{{1, 2}, {3, 4}},
			[]api.Span{{5, 6}, {7, 8}},
		},
		ExampleTermSpans: [][]api.Span{
			[]api.Span{{9, 10}, {11, 12}},
			[]api.Span{{13, 14}, {15, 16}},
		},
		CreationId: creationId,
	}
}

func createUserAuthToken(userId *primitive.ObjectID) *models.UserAuthToken {
	return &models.UserAuthToken{
		Id:          tools.Ptr(primitive.NewObjectID()),
		UserMongoId: userId,
		NetworkType: models.Google,
		AccessToken: models.AccessToken{
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
