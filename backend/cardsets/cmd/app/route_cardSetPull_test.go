package main

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"models/apphelpers"
	"models/card"
	"models/cardset"
	"models/test"
	"models/tools"
	"net/http"
	"net/http/httptest"
	"sort"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type CardSetPullTestSuite struct {
	suite.Suite
	BaseTestSuite
	application   *application
	pullValidator *test.MockSessionValidator[CardSetPullInput]
}

func (suite *CardSetPullTestSuite) SetupTest() {
	suite.pullValidator = test.NewMockSessionValidator[CardSetPullInput]()

	sessionManager := apphelpers.CreateSessionManager("172.16.0.3:6380")
	app, err := createApplication(
		true,
		sessionManager,
		"mongodb://127.0.0.1:27018/?directConnection=true&replicaSet=rs0",
		false,
		test.NewMockSessionValidator[CardSetPushInput](),
		suite.pullValidator,
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
		suite.createUUID().String(),
		time.Now().Add(-time.Hour*time.Duration(10)),
		[]*card.ApiCard{createApiCard(suite.createUUID().String())},
	)
	oldCardSet2 := createApiCardSet(
		"oldTestCardSet2",
		suite.createUUID().String(),
		time.Now(),
		[]*card.ApiCard{createApiCard(suite.createUUID().String())},
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

	suite.setupPullValidatorWithCardSetIds(userId, []string{})
	req, err := http.NewRequest("POST", "/", nil)
	if err != nil {
		suite.T().Fatal(err)
	}

	writer := httptest.NewRecorder()
	suite.application.cardSetPull(writer, req)
	response := suite.readPullResponse(writer)

	assert.Equal(suite.T(), http.StatusOK, writer.Code)
	assert.Len(suite.T(), response.DeletedCardSetIds, 0)
	assert.Len(suite.T(), response.UpdatedCardSets, 2)

	expectedCardSets := cardset.ApiCardSetSortByName([]*cardset.ApiCardSet{oldCardSet, oldCardSet2})
	sort.Sort(expectedCardSets)
	actualCardSets := cardset.ApiCardSetSortByName(response.UpdatedCardSets)
	sort.Sort(actualCardSets)

	assert.Equal(suite.T(), expectedCardSets, actualCardSets)
}

func (suite *CardSetPullTestSuite) TestCardSetPull_WithLastModificationDate_ReturnsOnlyNewCardSet() {
	oldCardSet := createApiCardSet(
		"oldTestCardSet1",
		suite.createUUID().String(),
		time.Now().Add(-time.Hour*time.Duration(20)),
		[]*card.ApiCard{createApiCard(suite.createUUID().String())},
	)

	oldCardSet2 := createApiCardSet(
		"oldTestCardSet2",
		suite.createUUID().String(),
		time.Now().Add(-time.Hour*time.Duration(10)),
		[]*card.ApiCard{createApiCard(suite.createUUID().String())},
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

	suite.setupPullValidatorWithCardSetIds(userId, []string{})
	req := suite.createPullRequest(time.Now().Add(-time.Hour * time.Duration(15)))

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

func (suite *CardSetPullTestSuite) setupPullValidatorWithCardSetIds(userId *primitive.ObjectID, cardSetIds []string) {
	suite.pullValidator.ResponseProvider = func() test.MockSessionValidatorResponse[CardSetPullInput] {
		return test.MockSessionValidatorResponse[CardSetPullInput]{
			&CardSetPullInput{
				"testAccessToken",
				cardSetIds,
			},
			createUserAuthToken(userId),
			nil,
		}
	}
}

func (suite *CardSetPullTestSuite) createPullRequest(lastModificationDate time.Time) *http.Request {
	req, err := http.NewRequest("POST", fmt.Sprintf("/?%s=%s", ParameterLatestCardSetModificationDate, lastModificationDate.UTC().Format(time.RFC3339)), nil)
	if err != nil {
		suite.T().Fatal(err)
	}
	req.AddCookie(&http.Cookie{Name: "session", Value: "testSession"})
	return req
}

func (suite *CardSetPullTestSuite) readPullResponse(writer *httptest.ResponseRecorder) *CardSetPullResponse {
	var response CardSetPullResponse
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
