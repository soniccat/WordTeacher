package storage

import (
	"context"
	"errors"
	"models"
	appUsearAuthToken "service_auth/internal/userauthtoken"
	"tools/logger"
	"tools/mongowrapper"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
)

type UserRepository struct {
	Logger         *logger.Logger
	UserCollection *mongo.Collection
	AuthTokens     *mongo.Collection
}

func NewUserRepository(logger *logger.Logger, mongoClient *mongo.Client) *UserRepository {
	database := mongoClient.Database(mongowrapper.MongoDatabaseUsers)
	model := &UserRepository{
		Logger:         logger,
		UserCollection: database.Collection(mongowrapper.MongoCollectionUsers),
		AuthTokens:     database.Collection(mongowrapper.MongoCollectionAuthTokens),
	}

	return model
}

func (m *UserRepository) FindUserById(context context.Context, networkType models.UserNetworkType, userId string) (*models.User, error) {
	var user = models.User{}

	err := m.UserCollection.FindOne(
		context,
		bson.M{
			"networks": bson.M{
				"$elemMatch": bson.M{
					"type":          networkType,
					"networkUserId": userId,
				},
			},
		},
	).Decode(&user)
	if errors.Is(err, mongo.ErrNoDocuments) {
		return nil, nil
	}

	return &user, err
}

func (m *UserRepository) InsertUser(context context.Context, user *models.User) (*models.User, error) {
	res, err := m.UserCollection.InsertOne(context, user)
	if err != nil {
		return nil, err
	}

	objId := res.InsertedID.(primitive.ObjectID)

	var newUser = *user
	newUser.Id = objId

	return &newUser, nil
}

// TODO: move to a service
func (m *UserRepository) GenerateUserAuthToken(
	context context.Context,
	userDbId string,
	networkType models.UserNetworkType,
	deviceType string,
	deviceId string,
) (*models.UserAuthToken, error) {
	token, err := appUsearAuthToken.GenerateUserAuthToken(userDbId, networkType, deviceType, deviceId)
	if err != nil {
		return nil, err
	}

	return m.insertUserAuthToken(context, token)
}

func (m *UserRepository) insertUserAuthToken(
	context context.Context,
	token *models.UserAuthToken,
) (*models.UserAuthToken, error) {
	// Remove stale service_auth tokens
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
	token.Id = objId.Hex()

	return token, nil
}

func (m *UserRepository) DropAll(context context.Context) error {
	err := m.UserCollection.Drop(context)
	if err != nil {
		return err
	}

	return m.AuthTokens.Drop(context)
}
