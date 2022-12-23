package main

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"models/apphelpers"
	"models/test"
	"net/http"
	"net/http/httptest"
	"testing"
)

type CardSetPullTestSuite struct {
	suite.Suite
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

func (suite *CardSetPushTestSuite) TestCardSetPull_WithoutAnything_ReturnsBadRequest() {
	req, err := http.NewRequest("POST", "/", nil)
	if err != nil {
		suite.T().Fatal(err)
	}

	writer := httptest.NewRecorder()
	suite.application.cardSetPull(writer, req)

	assert.Equal(suite.T(), http.StatusInternalServerError, writer.Code)
}

func TestCardSetPullTestSuite(t *testing.T) {
	suite.Run(t, new(CardSetPushTestSuite))
}
