package storage

import (
	"context"
	"models"
	"testing"
	"tools"
	"tools/test"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type UserModelTestSuite struct {
	suite.Suite
	UserModel *UserRepository
	TestMongo *test.TestMongo
}

func (suite *UserModelTestSuite) SetupTest() {
	suite.TestMongo = test.NewTestMongo()
	suite.UserModel = NewUserRepository(
		suite.TestMongo.GetLogger(),
		suite.TestMongo.GetMongoWrapper().Client,
	)
}

func (suite *UserModelTestSuite) TestUserCreationExample() {
	ctx := context.Background()
	user := &models.User{
		Networks: []models.UserNetwork{
			{
				models.Google,
				"testUserId",
				"testEmail",
				"testName",
			},
		},
	}

	insertedUser, err := suite.UserModel.InsertUser(ctx, user)
	assert.NoError(suite.T(), err)
	assert.NotNil(suite.T(), insertedUser.Id)

	loadedUser, err := suite.UserModel.FindGoogleUser(ctx, tools.Ptr("testUserId"))
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), insertedUser, loadedUser)
}

func TestUserModelTestSuite(t *testing.T) {
	suite.Run(t, new(UserModelTestSuite))
}