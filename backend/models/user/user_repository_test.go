package user

import (
	"context"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"models/test"
	"models/tools"
	"models/usernetwork"
	"testing"
)

type UserModelTestSuite struct {
	suite.Suite
	UserModel *UserRepository
	TestMongo *test.TestMongo
}

func (suite *UserModelTestSuite) SetupTest() {
	suite.TestMongo = test.NewTestMongo()
	suite.UserModel = New(
		suite.TestMongo.GetLogger(),
		suite.TestMongo.GetMongoWrapper().Client,
	)
}

func (suite *UserModelTestSuite) TestUserCreationExample() {
	ctx := context.Background()
	user := &User{
		Networks: []usernetwork.UserNetwork{
			{
				usernetwork.Google,
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
