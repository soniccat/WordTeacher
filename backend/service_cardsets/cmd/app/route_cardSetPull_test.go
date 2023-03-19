package main

import (
	"api"
	"bytes"
	"context"
	"fmt"
	"models/helpers"
	"net/http"
	"net/http/httptest"
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
	sessionValidator *helpers.MockSessionValidator
}

func (suite *CardSetPullTestSuite) SetupTest() {
	suite.sessionValidator = helpers.NewMockSessionValidator()

	sessionManager := tools.CreateSessionManager("172.16.0.3:6380")
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

func (suite *CardSetPullTestSuite) TestCardSetPull_WithoutAnything_ReturnsBadRequest() {
	req, err := http.NewRequest("POST", "/", nil)
	if err != nil {
		suite.T().Fatal(err)
	}

	writer := httptest.NewRecorder()
	suite.application.cardSetPull(writer, req)

	assert.Equal(suite.T(), http.StatusInternalServerError, writer.Code)
}

func (suite *CardSetPullTestSuite) TestCardSetPull_WithoutLastModificationDate_ReturnsAllCardSets() {
	oldCardSet := createApiCardSet(
		"oldTestCardSet1",
		suite.CreateUUID().String(),
		time.Now().Add(-time.Hour*time.Duration(10)),
		[]*api.ApiCard{createApiCard(suite.CreateUUID().String())},
	)
	oldCardSet2 := createApiCardSet(
		"oldTestCardSet2",
		suite.CreateUUID().String(),
		time.Now(),
		[]*api.ApiCard{createApiCard(suite.CreateUUID().String())},
	)

	userId := tools.Ptr(primitive.NewObjectID())
	_, errWithCode := suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet, userId)
	if errWithCode != nil {
		suite.T().Fatal(errWithCode.Err)
	}
	_, errWithCode = suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet2, userId)
	if errWithCode != nil {
		suite.T().Fatal(errWithCode.Err)
	}

	suite.setupPullValidator(userId)

	req := suite.createPullRequest(nil, CardSetPullInput{[]string{}})
	writer := httptest.NewRecorder()
	suite.application.cardSetPull(writer, req)
	response := suite.readPullResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
	assert.Len(suite.T(), response.DeletedCardSetIds, 0)
	assert.Len(suite.T(), response.UpdatedCardSets, 2)

	expectedCardSets := api.ApiCardSetSortByName([]*api.ApiCardSet{oldCardSet, oldCardSet2})
	sort.Sort(expectedCardSets)
	actualCardSets := api.ApiCardSetSortByName(response.UpdatedCardSets)
	sort.Sort(actualCardSets)

	assert.Equal(suite.T(), expectedCardSets, actualCardSets)
}

func (suite *CardSetPullTestSuite) TestCardSetPull_WithLastModificationDate_ReturnsOnlyNewCardSet() {
	oldCardSet := createApiCardSet(
		"oldTestCardSet1",
		suite.CreateUUID().String(),
		time.Now().Add(-time.Hour*time.Duration(20)),
		[]*api.ApiCard{createApiCard(suite.CreateUUID().String())},
	)

	oldCardSet2 := createApiCardSet(
		"oldTestCardSet2",
		suite.CreateUUID().String(),
		time.Now().Add(-time.Hour*time.Duration(10)),
		[]*api.ApiCard{createApiCard(suite.CreateUUID().String())},
	)

	userId := tools.Ptr(primitive.NewObjectID())
	_, errWithCode := suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet, userId)
	if errWithCode != nil {
		suite.T().Fatal(errWithCode.Err)
	}
	_, errWithCode = suite.application.cardSetRepository.InsertCardSet(context.Background(), oldCardSet2, userId)
	if errWithCode != nil {
		suite.T().Fatal(errWithCode.Err)
	}

	suite.setupPullValidator(userId)
	req := suite.createPullRequest(tools.Ptr(time.Now().Add(-time.Hour*time.Duration(15))), CardSetPullInput{})

	writer := httptest.NewRecorder()
	suite.application.cardSetPull(writer, req)
	response := suite.readPullResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
	assert.Len(suite.T(), response.DeletedCardSetIds, 0)
	assert.Len(suite.T(), response.UpdatedCardSets, 1)
	assert.Equal(suite.T(), oldCardSet2, response.UpdatedCardSets[0])
}

func TestCardSetPullTestSuite(t *testing.T) {
	suite.Run(t, new(CardSetPullTestSuite))
}

// Tools

func (suite *CardSetPullTestSuite) setupPullValidator(userId *primitive.ObjectID) {
	suite.sessionValidator.ResponseProvider = func() helpers.MockSessionValidatorResponse {
		return helpers.MockSessionValidatorResponse{
			createUserAuthToken(userId),
			nil,
		}
	}
}

func (suite *CardSetPullTestSuite) createPullRequest(lastModificationDate *time.Time, input CardSetPullInput) *http.Request {
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

func (suite *CardSetPullTestSuite) readPullResponse(writer *httptest.ResponseRecorder) *CardSetPullResponse {
	return test.TestReadResponse[CardSetPullResponse](writer)
}
