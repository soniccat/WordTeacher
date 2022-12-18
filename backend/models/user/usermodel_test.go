package user

import (
	"context"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"models/logger"
	"models/mongowrapper"
	"models/tools"
	"models/usernetwork"
	"testing"
)

// Define the suite, and absorb the built-in basic suite
// functionality from testify - including a T() method which
// returns the current testing context
type ExampleTestSuite struct {
	suite.Suite
	UserModel                     *UserModel
	VariableThatShouldStartAtFive int
}

type TestMongo struct {
	logger       *logger.Logger
	mongoWrapper *mongowrapper.MongoWrapper
	userModel    *UserModel
}

func (a *TestMongo) GetLogger() *logger.Logger {
	return a.logger
}

func (a *TestMongo) SetMongoWrapper(mw *mongowrapper.MongoWrapper) {
	a.mongoWrapper = mw
}

// Make sure that VariableThatShouldStartAtFive is set to five
// before each test
func (suite *ExampleTestSuite) SetupTest() {
	suite.VariableThatShouldStartAtFive = 5

	lg := logger.New(true)

	mongoApp := &TestMongo{
		logger: logger.New(true),
	}
	err := mongowrapper.SetupMongo(mongoApp, tools.Ptr("mongodb://127.0.0.1:27017/?directConnection=true&replicaSet=rs0"), tools.Ptr(false))
	if err != nil {
		panic(err)
	}

	usersDatabase := mongoApp.mongoWrapper.Client.Database(mongowrapper.MongoDatabaseUsers)

	suite.UserModel = New(
		lg,
		usersDatabase,
	)
}

// All methods that begin with "Test" are run as tests within a
// suite.
func (suite *ExampleTestSuite) TestExample() {
	assert.Equal(suite.T(), 5, suite.VariableThatShouldStartAtFive)
}

func (suite *ExampleTestSuite) TestUserCreationExample() {
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
	assert.NotNil(suite.T(), insertedUser.ID)

	loadedUser, err := suite.UserModel.FindGoogleUser(ctx, tools.Ptr(insertedUser.ID.Hex()))
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), insertedUser, loadedUser)
}

// In order for 'go test' to run this suite, we need to create
// a normal test function and pass our suite to suite.Run
func TestExampleTestSuite(t *testing.T) {
	suite.Run(t, new(ExampleTestSuite))
}

//func TestSomething(t *testing.T) {
//
//	assert.True(t, true, "True is true!")
//
//}
