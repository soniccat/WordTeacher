package user

import (
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"models/logger"
	"models/mongowrapper"
	"models/userauthtoken"
	"models/usernetwork"
)

// TODO: move in auth module
type UserRepository struct {
	Logger         *logger.Logger
	UserCollection *mongo.Collection
	AuthTokens     *mongo.Collection
}

func New(logger *logger.Logger, mongoClient *mongo.Client) *UserRepository {
	database := mongoClient.Database(mongowrapper.MongoDatabaseUsers)
	model := &UserRepository{
		Logger:         logger,
		UserCollection: database.Collection(mongowrapper.MongoCollectionUsers),
		AuthTokens:     database.Collection(mongowrapper.MongoCollectionAuthTokens),
	}

	return model
}

func (m *UserRepository) FindGoogleUser(context context.Context, googleUserId *string) (*User, error) {
	var user = User{}

	err := m.UserCollection.FindOne(
		context,
		bson.M{
			"networks": bson.M{
				"$elemMatch": bson.M{
					"type":          usernetwork.Google,
					"networkUserId": *googleUserId,
				},
			},
		},
	).Decode(&user)
	if err == mongo.ErrNoDocuments {
		return nil, nil
	}

	return &user, err
}

func (m *UserRepository) InsertUser(context context.Context, user *User) (*User, error) {
	res, err := m.UserCollection.InsertOne(context, user)
	if err != nil {
		return nil, err
	}

	objId := res.InsertedID.(primitive.ObjectID)

	var newUser = *user
	newUser.Id = objId

	return &newUser, nil
}

// TODO: move in auth module
func (m *UserRepository) GenerateUserAuthToken(
	context context.Context,
	userId *primitive.ObjectID,
	networkType usernetwork.UserNetworkType,
	deviceType string,
	deviceId string,
) (*userauthtoken.UserAuthToken, error) {
	token, err := userauthtoken.Generate(userId, networkType, deviceType, deviceId)
	if err != nil {
		return nil, err
	}

	return m.insertUserAuthToken(context, token)
}

// TODO: move in auth module
func (m *UserRepository) insertUserAuthToken(
	context context.Context,
	token *userauthtoken.UserAuthToken,
) (*userauthtoken.UserAuthToken, error) {
	// Remove stale auth tokens
	_, err := m.AuthTokens.DeleteMany(
		context,
		bson.M{
			"deviceId": token.UserDeviceId,
		},
	)
	if err != nil {
		m.Logger.Error.Printf("InsertUserToken DeleteMany error %s", err.Error())
	}

	// Add the new one
	res, err := m.AuthTokens.InsertOne(
		context,
		token,
	)
	if err != nil {
		return nil, err
	}

	objId := res.InsertedID.(primitive.ObjectID)

	token.Id = &objId
	return token, nil
}

type ValidateSessionError struct {
	StatusCode int
	InnerError error
}

type TokenHolder[B any] interface {
	GetAccessToken() string
	GetRefreshToken() *string
	*B // non-interface type constraint element
}

func NewValidateSessionError(code int, err error) *ValidateSessionError {
	return &ValidateSessionError{
		StatusCode: code,
		InnerError: err,
	}
}

func (v *ValidateSessionError) Error() string {
	return v.InnerError.Error()
}
