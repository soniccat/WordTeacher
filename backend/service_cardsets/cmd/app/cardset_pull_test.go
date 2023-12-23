package main

import (
	"api"
	"bytes"
	"context"
	"models/session_validator"
	"net/http"
	"net/http/httptest"
	"service_cardsets/internal/routing/cardset_pull"
	"service_cardsets/internal/storage"
	"sort"
	"testing"
	"time"
	"tools"
	"tools/test"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type CardSetPullTestSuite struct {
	suite.Suite
	test.BaseTestSuite
	application      *application
	sessionValidator *session_validator.MockSessionValidator
}

func (suite *CardSetPullTestSuite) SetupTest() {
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

func (suite *CardSetPullTestSuite) TestCardSetPull_WithoutAnything_ReturnsBadRequest() {
	req, err := http.NewRequest("POST", "/", nil)
	if err != nil {
		suite.T().Fatal(err)
	}

	cardSetPullHandler := cardset_pull.NewHandler(
		suite.Logger(),
		suite.TestTimeProvider(),
		suite.application.sessionValidator,
		suite.application.cardSetRepository,
	)

	writer := httptest.NewRecorder()
	cardSetPullHandler.CardSetPull(writer, req)

	assert.Equal(suite.T(), http.StatusInternalServerError, writer.Code)
}

func (suite *CardSetPullTestSuite) TestCardSetPull_WithoutLastModificationDate_ReturnsAllCardSets() {
	creationTime1 := time.Now().Add(-time.Hour * time.Duration(10))
	oldCardSet := createApiCardSet(
		"oldTestCardSet1",
		suite.CreateUUID().String(),
		creationTime1,
		[]*api.Card{createApiCard(suite.CreateUUID().String())},
	)
	creationTime2 := creationTime1.Add(time.Hour * time.Duration(2))
	oldCardSet2 := createApiCardSet(
		"oldTestCardSet2",
		suite.CreateUUID().String(),
		creationTime2,
		[]*api.Card{createApiCard(suite.CreateUUID().String())},
	)

	userId := primitive.NewObjectID().Hex()
	_, err := suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet, userId)
	if err != nil {
		suite.T().Fatal(err)
	}
	_, err = suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet2, userId)
	if err != nil {
		suite.T().Fatal(err)
	}

	suite.setupPullValidator(userId)

	cardSetPullHandler := cardset_pull.NewHandler(
		suite.Logger(),
		suite.TestTimeProvider(),
		suite.application.sessionValidator,
		suite.application.cardSetRepository,
	)

	req := suite.createPullRequest(nil, cardset_pull.Input{CurrentCardSetIds: []string{}})
	writer := httptest.NewRecorder()
	cardSetPullHandler.CardSetPull(writer, req)
	response := suite.readPullResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
	assert.Len(suite.T(), response.DeletedCardSetIds, 0)
	assert.Len(suite.T(), response.UpdatedCardSets, 2)

	expectedCardSets := api.CardSetSortByName([]*api.CardSet{oldCardSet, oldCardSet2})
	sort.Sort(expectedCardSets)
	actualCardSets := api.CardSetSortByName(response.UpdatedCardSets)
	sort.Sort(actualCardSets)

	assert.Equal(suite.T(), expectedCardSets, actualCardSets)
	assert.Equal(suite.T(), tools.TimeToApiDate(creationTime2), response.LatestModificationDate)
}

func (suite *CardSetPullTestSuite) TestCardSetPull_WithLastModificationDate_ReturnsOnlyNewCardSet() {
	creationTime1 := time.Now().Add(-time.Hour * time.Duration(20))
	oldCardSet := createApiCardSet(
		"oldTestCardSet1",
		suite.CreateUUID().String(),
		creationTime1,
		[]*api.Card{createApiCard(suite.CreateUUID().String())},
	)

	creationTime2 := creationTime1.Add(time.Hour * time.Duration(10))
	oldCardSet2 := createApiCardSet(
		"oldTestCardSet2",
		suite.CreateUUID().String(),
		creationTime2,
		[]*api.Card{createApiCard(suite.CreateUUID().String())},
	)

	userId := primitive.NewObjectID().Hex()
	_, err := suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet, userId)
	if err != nil {
		suite.T().Fatal(err)
	}
	_, err = suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet2, userId)
	if err != nil {
		suite.T().Fatal(err)
	}

	suite.setupPullValidator(userId)
	req := suite.createPullRequest(tools.Ptr(creationTime2.Add(-time.Hour*time.Duration(5))), cardset_pull.Input{})

	cardSetPullHandler := cardset_pull.NewHandler(
		suite.Logger(),
		suite.TestTimeProvider(),
		suite.application.sessionValidator,
		suite.application.cardSetRepository,
	)

	writer := httptest.NewRecorder()
	cardSetPullHandler.CardSetPull(writer, req)
	response := suite.readPullResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
	assert.Len(suite.T(), response.DeletedCardSetIds, 0)
	assert.Len(suite.T(), response.UpdatedCardSets, 1)
	assert.Equal(suite.T(), oldCardSet2, response.UpdatedCardSets[0])
	assert.Equal(suite.T(), tools.TimeToApiDate(creationTime2), response.LatestModificationDate)
}

func TestCardSetPullTestSuite(t *testing.T) {
	suite.Run(t, new(CardSetPullTestSuite))
}

// Tools

func (suite *CardSetPullTestSuite) setupPullValidator(userId string) {
	suite.sessionValidator.ResponseProvider = func() session_validator.MockSessionValidatorResponse {
		return session_validator.MockSessionValidatorResponse{
			AuthToken:            createUserAuthToken(userId),
			ValidateSessionError: nil,
		}
	}
}

func (suite *CardSetPullTestSuite) createPullRequest(lastModificationDate *time.Time, input cardset_pull.Input) *http.Request {
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

func (suite *CardSetPullTestSuite) readPullResponse(writer *httptest.ResponseRecorder) *cardset_pull.Response {
	return test.TestReadResponse[cardset_pull.Response](writer)
}
