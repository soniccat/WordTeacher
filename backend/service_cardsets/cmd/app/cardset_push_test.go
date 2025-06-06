package main

import (
	"api"
	"bytes"
	"context"
	"errors"
	"models"
	"models/session_validator"
	"net/http"
	"net/http/httptest"
	"service_cardsets/internal/model"
	"service_cardsets/internal/routing/cardset_push"
	"service_cardsets/internal/storage"
	"testing"
	"time"
	"tools"
	"tools/test"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type CardSetPushTestSuite struct {
	suite.Suite
	test.BaseTestSuite
	application      *application
	sessionValidator *session_validator.MockSessionValidator
}

func (suite *CardSetPushTestSuite) SetupTest() {
	suite.sessionValidator = session_validator.NewMockSessionValidator()
	sessionManager := tools.CreateSessionManager(test.RedisIP)
	storage, err := storage.New(
		suite.Logger(),
		test.MongoURI,
		false,
	)
	if err != nil {
		suite.T().Fatal(err)
	}

	app, err := createApplication(
		suite.Logger(),
		suite.TestTimeProvider(),
		sessionManager,
		suite.sessionValidator,
		storage,
	)
	if err != nil {
		suite.T().Fatal(err)
	}

	suite.application = app
	app.cardSetRepository.DropAll(context.Background())
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithoutAnything_ReturnsBadRequest() {
	req, err := http.NewRequest("POST", "/", nil)
	if err != nil {
		suite.T().Fatal(err)
	}

	cardSetPushHandler := cardset_push.NewHandler(
		suite.Logger(),
		suite.TestTimeProvider(),
		suite.application.sessionValidator,
		suite.application.cardSetRepository,
	)

	writer := httptest.NewRecorder()
	cardSetPushHandler.CardSetPush(writer, req)

	assert.Equal(suite.T(), http.StatusInternalServerError, writer.Code)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithCookieButWithoutLastModificationDate_ReturnsBadRequest() {
	req, err := http.NewRequest("POST", "/", nil)
	req.AddCookie(&http.Cookie{Name: "session", Value: "testSession"})
	if err != nil {
		suite.T().Fatal(err)
	}

	cardSetPushHandler := cardset_push.NewHandler(
		suite.Logger(),
		suite.TestTimeProvider(),
		suite.application.sessionValidator,
		suite.application.cardSetRepository,
	)

	writer := httptest.NewRecorder()
	cardSetPushHandler.CardSetPush(writer, req)

	assert.Equal(suite.T(), http.StatusInternalServerError, writer.Code)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithInvalidSession_ReturnsUnauthorized() {
	suite.sessionValidator.ResponseProvider = func() session_validator.MockSessionValidatorResponse {
		return session_validator.MockSessionValidatorResponse{
			AuthToken:            nil,
			ValidateSessionError: session_validator.NewValidateSessionError(http.StatusUnauthorized, errors.New("test error")),
		}
	}

	req := suite.createPushRequest(&time.Time{}, cardset_push.Input{})
	req.AddCookie(&http.Cookie{Name: "session", Value: "testSession"})

	cardSetPushHandler := cardset_push.NewHandler(
		suite.Logger(),
		suite.TestTimeProvider(),
		suite.application.sessionValidator,
		suite.application.cardSetRepository,
	)

	writer := httptest.NewRecorder()
	cardSetPushHandler.CardSetPush(writer, req)

	assert.Equal(suite.T(), http.StatusUnauthorized, writer.Code)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithNewCardSet_ReturnsOk() {
	cardSetCreationIdUUID := suite.CreateUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	cardCreationId := suite.CreateUUID().String()

	createTime := time.Now().Add(time.Second * time.Duration(1000))
	newCardSet := createApiCardSet(
		"testCardSet",
		cardSetCreationId,
		createTime,
		[]*api.Card{createApiCard(cardCreationId, createTime)},
	)

	suite.setupPushValidator(primitive.NewObjectID().Hex())

	cardSetPushHandler := cardset_push.NewHandler(
		suite.Logger(),
		suite.TestTimeProvider(),
		suite.application.sessionValidator,
		suite.application.cardSetRepository,
	)

	req := suite.createPushRequest(
		tools.Ptr(time.Now()),
		cardset_push.Input{UpdatedCardSets: []*api.CardSet{newCardSet}, CurrentCardSetIds: []string{}},
	)

	writer := httptest.NewRecorder()

	cardSetPushHandler.CardSetPush(writer, req)
	response := suite.readPushResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
	assert.Len(suite.T(), response.CardSetIds, 1)
	assert.Equal(suite.T(), cardSetCreationId, tools.MapKeys(response.CardSetIds)[0])
	assert.NotNil(suite.T(), cardSetCreationId, response.CardSetIds[cardSetCreationId])
	assert.Len(suite.T(), response.CardIds, 1)
	assert.Equal(suite.T(), cardCreationId, tools.MapKeys(response.CardIds)[0])
	assert.Equal(suite.T(), tools.TimeToApiDate(createTime), response.LatestModificationDate)

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

	createTime := time.Now().Add(time.Second * time.Duration(-1000))
	newCardSet := createApiCardSet(
		"testCardSet",
		cardSetCreationId,
		createTime,
		[]*api.Card{createApiCard(apiCardCreationId, createTime)},
	)

	userId := primitive.NewObjectID().Hex()
	insertedCardSet, err := suite.application.cardSetRepository.InsertCardSet(context.Background(), newCardSet, userId)
	if err != nil {
		suite.T().Fatal(err)
	}

	modificationTime := createTime.Add(time.Second * time.Duration(500))
	newCardSet = createApiCardSet(
		"testCardSet",
		cardSetCreationId,
		modificationTime,
		[]*api.Card{createApiCard(apiCardCreationId, createTime)},
	)

	suite.setupPushValidator(userId)
	cardSetPushHandler := cardset_push.NewHandler(
		suite.Logger(),
		suite.TestTimeProvider(),
		suite.application.sessionValidator,
		suite.application.cardSetRepository,
	)

	latestModificationTime := time.Now()
	req := suite.createPushRequest(
		tools.Ptr(latestModificationTime),
		cardset_push.Input{UpdatedCardSets: []*api.CardSet{newCardSet}, CurrentCardSetIds: []string{}},
	)

	writer := httptest.NewRecorder()
	cardSetPushHandler.CardSetPush(writer, req)
	response := suite.readPushResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
	assert.Equal(suite.T(), insertedCardSet.Id, response.CardSetIds[cardSetCreationId])
	assert.Equal(suite.T(), tools.TimeToApiDate(latestModificationTime), response.LatestModificationDate)

	dbCardSet := suite.loadCardSetDbById(response.CardSetIds[cardSetCreationId])
	assert.NotNil(suite.T(), dbCardSet.UserId)
	assert.Equal(suite.T(), newCardSet, dbCardSet.ToApi().WithoutIDs())
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithNewCardSetAndOldOne_ReturnsNewCardSetAndDeleteOldOne() {
	creationTime := time.Now().Add(time.Hour * time.Duration(-10))
	oldCardSet := createApiCardSet(
		"oldTestCardSet",
		suite.CreateUUID().String(),
		creationTime,
		[]*api.Card{createApiCard(suite.CreateUUID().String(), creationTime)},
	)

	userId := primitive.NewObjectID().Hex()
	_, err := suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet, userId)
	if err != nil {
		suite.T().Fatal(err)
	}

	cardSetCreationIdUUID := suite.CreateUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	newCreationTime := creationTime.Add(time.Hour * time.Duration(5))
	newCardSet := createApiCardSet(
		"newTestCardSet",
		cardSetCreationId,
		newCreationTime,
		[]*api.Card{createApiCard(suite.CreateUUID().String(), newCreationTime)},
	)

	suite.setupPushValidator(userId)
	cardSetPushHandler := cardset_push.NewHandler(
		suite.Logger(),
		suite.TestTimeProvider(),
		suite.application.sessionValidator,
		suite.application.cardSetRepository,
	)

	lastSyncTime := newCreationTime.Add(time.Hour * time.Duration(-1))
	req := suite.createPushRequest(
		tools.Ptr(lastSyncTime),
		cardset_push.Input{UpdatedCardSets: []*api.CardSet{newCardSet}, CurrentCardSetIds: []string{}},
	)

	nowTime := newCreationTime.Add(time.Hour * time.Duration(10))
	suite.TestTimeProvider().Time = nowTime
	writer := httptest.NewRecorder()
	cardSetPushHandler.CardSetPush(writer, req)
	response := suite.readPushResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)

	dbCardSet := suite.loadCardSetDbById(response.CardSetIds[cardSetCreationId])
	assert.Equal(suite.T(), newCardSet, dbCardSet.ToApi().WithoutIDs())

	oo, _ := suite.application.cardSetRepository.LoadCardSetDbById(context.Background(), oldCardSet.Id)
	assert.Equal(suite.T(), true, oo.IsDeleted)
	assert.Equal(suite.T(), tools.TimeToApiDate(nowTime), response.LatestModificationDate)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_WithNotPulledChanges_ReturnsStatusConflict() {
	creationTime := time.Now()
	newCardSet := createApiCardSet(
		"newTestCardSet",
		suite.CreateUUID().String(),
		creationTime,
		[]*api.Card{createApiCard(suite.CreateUUID().String(), creationTime)},
	)

	userId := primitive.NewObjectID().Hex()
	_, err := suite.application.cardSetRepository.InsertCardSet(context.Background(), newCardSet, userId)
	if err != nil {
		suite.T().Fatal(err)
	}

	cardSetCreationIdUUID := suite.CreateUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	cardCreationId := suite.CreateUUID().String()
	oldCreationTime := time.Now()
	oldCardSet := createApiCardSet(
		"oldTestCardSet",
		cardSetCreationId,
		oldCreationTime,
		[]*api.Card{createApiCard(cardCreationId, oldCreationTime)},
	)

	suite.setupPushValidator(userId)
	cardSetPushHandler := cardset_push.NewHandler(
		suite.Logger(),
		suite.TestTimeProvider(),
		suite.application.sessionValidator,
		suite.application.cardSetRepository,
	)

	req := suite.createPushRequest(
		tools.Ptr(time.Now().Add(time.Hour*time.Duration(-20))),
		cardset_push.Input{UpdatedCardSets: []*api.CardSet{oldCardSet}, CurrentCardSetIds: []string{}},
	)

	writer := httptest.NewRecorder()
	cardSetPushHandler.CardSetPush(writer, req)

	assert.Equal(suite.T(), http.StatusConflict, writer.Code)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_NewCardSetWithExistingCardSet_ReturnsOk() {
	creationTime := time.Now().Add(time.Hour * time.Duration(-20))
	oldCardSet := createApiCardSet(
		"oldTestCardSet",
		suite.CreateUUID().String(),
		creationTime,
		[]*api.Card{createApiCard(suite.CreateUUID().String(), creationTime)},
	)

	userId := primitive.NewObjectID().Hex()
	_, err := suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet, userId)
	if err != nil {
		suite.T().Fatal(err)
	}

	cardSetCreationIdUUID := suite.CreateUUID()
	cardSetCreationId := cardSetCreationIdUUID.String()
	cardCreationId := suite.CreateUUID().String()
	modificationTime := creationTime.Add(time.Hour * time.Duration(10))
	newCardSet := createApiCardSet(
		"newTestCardSet",
		cardSetCreationId,
		modificationTime,
		[]*api.Card{createApiCard(cardCreationId, modificationTime)},
	)

	suite.setupPushValidator(userId)
	cardSetPushHandler := cardset_push.NewHandler(
		suite.Logger(),
		suite.TestTimeProvider(),
		suite.application.sessionValidator,
		suite.application.cardSetRepository,
	)

	lastChangeTime := modificationTime.Add(-time.Hour * time.Duration(5))
	req := suite.createPushRequest(
		tools.Ptr(lastChangeTime),
		cardset_push.Input{UpdatedCardSets: []*api.CardSet{newCardSet}, CurrentCardSetIds: []string{}},
	)

	writer := httptest.NewRecorder()

	nowTime := modificationTime.Add(time.Duration(time.Hour * 5))
	suite.TestTimeProvider().Time = nowTime
	cardSetPushHandler.CardSetPush(writer, req)
	response := suite.readPushResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
	assert.Len(suite.T(), response.CardSetIds, 1)
	assert.Equal(suite.T(), cardSetCreationId, tools.MapKeys(response.CardSetIds)[0])
	assert.NotEmpty(suite.T(), cardSetCreationId, response.CardSetIds[cardSetCreationId])
	assert.Len(suite.T(), response.CardIds, 1)
	assert.Equal(suite.T(), cardCreationId, tools.MapKeys(response.CardIds)[0])
	assert.Equal(suite.T(), tools.TimeToApiDate(nowTime), response.LatestModificationDate)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
}

func (suite *CardSetPushTestSuite) TestCardSetPush_PushEmptyChangesWithOldDate_ReturnsStatusConflict() {
	t := time.Now()
	card1 := createApiCard(suite.CreateUUID().String(), t)
	card1.Term = "my term"
	card1.ModificationDate = tools.TimeToApiDate(t)
	card2 := createApiCard(suite.CreateUUID().String(), t)
	newCardSet := createApiCardSet(
		"newTestCardSet",
		suite.CreateUUID().String(),
		t,
		[]*api.Card{
			card1,
			card2,
		},
	)

	userId := primitive.NewObjectID().Hex()
	_, err := suite.application.cardSetRepository.InsertCardSet(context.Background(), newCardSet, userId)
	if err != nil {
		suite.T().Fatal(err)
	}

	suite.setupPushValidator(userId)
	cardSetPushHandler := cardset_push.NewHandler(
		suite.Logger(),
		suite.TestTimeProvider(),
		suite.application.sessionValidator,
		suite.application.cardSetRepository,
	)

	// client1 updates card
	card1.Term = "my term 2"
	card1.ModificationDate = tools.TimeToApiDate(t.Add(time.Minute * time.Duration(4)))
	newCardSet.Cards = []*api.Card{card1, card2}
	newCardSet.ModificationDate = tools.TimeToApiDate(t.Add(time.Minute * time.Duration(4)))
	reqUpdate := suite.createPushRequest(
		tools.Ptr(t.Add(time.Minute*time.Duration(2))),
		cardset_push.Input{UpdatedCardSets: []*api.CardSet{
			newCardSet,
		}, CurrentCardSetIds: []string{newCardSet.Id}},
	)
	reqUpdateWriter := httptest.NewRecorder()
	cardSetPushHandler.CardSetPush(reqUpdateWriter, reqUpdate)
	assert.Equal(suite.T(), http.StatusOK, reqUpdateWriter.Code)

	// client2 pushes nothing
	reqPushEmpty := suite.createPushRequest(
		tools.Ptr(t.Add(time.Minute*time.Duration(3))),
		cardset_push.Input{UpdatedCardSets: []*api.CardSet{}, CurrentCardSetIds: []string{newCardSet.Id}},
	)

	reqPushEmptyWriter := httptest.NewRecorder()
	cardSetPushHandler.CardSetPush(reqPushEmptyWriter, reqPushEmpty)

	assert.Equal(suite.T(), http.StatusConflict, reqPushEmptyWriter.Code)
}

// Tools

func (suite *CardSetPushTestSuite) loadCardSetDbById(id string) *model.DbCardSet {
	dbCardSet, err := suite.application.cardSetRepository.LoadCardSetDbById(context.Background(), id)
	if err != nil {
		suite.T().Fatal(err)
	}
	return dbCardSet
}

func (suite *CardSetPushTestSuite) createPushRequest(lastModificationDate *time.Time, input cardset_push.Input) *http.Request {
	var resultPath = ""
	if lastModificationDate != nil {
		input.LatestModificationDate = tools.Ptr(tools.TimeToApiDate(*lastModificationDate))
	}

	req, err := http.NewRequest("POST", resultPath, bytes.NewReader(test.TestMarshal(input)))
	if err != nil {
		suite.T().Fatal(err)
	}

	req.AddCookie(&http.Cookie{Name: "session", Value: "testSession"})
	return req
}

func (suite *CardSetPushTestSuite) setupPushValidator(userId string) {
	suite.sessionValidator.ResponseProvider = func() session_validator.MockSessionValidatorResponse {
		return session_validator.MockSessionValidatorResponse{
			AuthToken:            createUserAuthToken(userId),
			ValidateSessionError: nil,
		}
	}
}

func (suite *CardSetPushTestSuite) readPushResponse(writer *httptest.ResponseRecorder) *cardset_push.Response {
	return test.TestReadResponse[cardset_push.Response](writer)
}

func createApiCardSet(name string, creationId string, creationDate time.Time, cards []*api.Card) *api.CardSet {
	return &api.CardSet{
		Name:                name,
		Cards:               cards,
		CreationDate:        tools.TimeToApiDate(creationDate),
		ModificationDate:    tools.TimeToApiDate(creationDate),
		CreationId:          creationId,
		Info:                api.CardSetInfo{},
		IsAvailableInSearch: false,
	}
}

func createApiCard(creationId string, creationDate time.Time) *api.Card {
	return &api.Card{
		Term:           "testTerm1",
		Transcription:  tools.Ptr("testTranscription"),
		Transcriptions: []string{"testTranscription", "testTranscription2"},
		AudioFiles: []api.AudioFile{
			{
				Url:           "test.ru",
				Accent:        tools.Ptr("audioAccent"),
				Transcription: tools.Ptr("audioTranscription"),
				Text:          tools.Ptr("audioText"),
			},
		},
		PartOfSpeech: api.Adverb,
		Definitions:  []string{"testDef1", "testDef2"},
		Synonyms:     []string{"testSyn1", "testSyn2"},
		Examples:     []string{"testEx1", "testEx2"},
		DefinitionTermSpans: [][]api.Span{
			{{Start: 1, End: 2}, {Start: 3, End: 4}},
			{{Start: 5, End: 6}, {Start: 7, End: 8}},
		},
		ExampleTermSpans: [][]api.Span{
			{{Start: 9, End: 10}, {Start: 11, End: 12}},
			{{Start: 13, End: 14}, {Start: 15, End: 16}},
		},
		CreationId:       creationId,
		CreationDate:     tools.TimeToApiDate(creationDate),
		ModificationDate: tools.TimeToApiDate(creationDate),
	}
}

func createUserAuthToken(userId string) *models.UserAuthToken {
	return &models.UserAuthToken{
		Id:          primitive.NewObjectID().Hex(),
		UserDbId:    userId,
		NetworkType: models.Google,
		AccessToken: models.AccessToken{
			Value:          "testAccessToken",
			ExpirationDate: time.Now(),
		},
		RefreshToken: models.RefreshToken{
			Value:          "testRefreshToken",
			ExpirationDate: time.Now(),
		},
		UserDeviceId: "testDeviceId",
	}
}

func TestCardSetPushTestSuite(t *testing.T) {
	suite.Run(t, new(CardSetPushTestSuite))
}
