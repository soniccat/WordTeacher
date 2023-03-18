package internal

import (
	appUsearAuthToken "auth/internal/userauthtoken"
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"models/user"
	"models/userauthtoken"
	"models/usernetwork"
	"tools/logger"
	"tools/mongowrapper"
)

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

func (m *UserRepository) FindGoogleUser(context context.Context, googleUserId *string) (*user.User, error) {
	var user = user.User{}

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

func (m *UserRepository) InsertUser(context context.Context, user *user.User) (*user.User, error) {
	res, err := m.UserCollection.InsertOne(context, user)
	if err != nil {
		return nil, err
	}

	objId := res.InsertedID.(primitive.ObjectID)

	var newUser = *user
	newUser.Id = objId

	return &newUser, nil
}

func (m *UserRepository) GenerateUserAuthToken(
	context context.Context,
	userId *primitive.ObjectID,
	networkType usernetwork.UserNetworkType,
	deviceType string,
	deviceId string,
) (*userauthtoken.UserAuthToken, error) {
	token, err := appUsearAuthToken.GenerateUserAuthToken(userId, networkType, deviceType, deviceId)
	if err != nil {
		return nil, err
	}

	return m.insertUserAuthToken(context, token)
}

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
