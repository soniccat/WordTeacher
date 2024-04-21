package storage

import (
	"context"
	"errors"
	"models"
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

func (m *UserRepository) FindUserById(ctx context.Context, networkType models.UserNetworkType, userId string) (*models.User, error) {
	var user = models.User{}

	err := m.UserCollection.FindOne(
		ctx,
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

	return &user, logger.WrapError(ctx, err)
}

func (m *UserRepository) InsertUser(ctx context.Context, user *models.User) (*models.User, error) {
	res, err := m.UserCollection.InsertOne(ctx, user)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	objId := res.InsertedID.(primitive.ObjectID)

	var newUser = *user
	newUser.Id = objId

	return &newUser, nil
}

func (m *UserRepository) InsertUserAuthToken(
	ctx context.Context,
	token *models.UserAuthToken,
) (*models.UserAuthToken, error) {
	// Remove stale service_auth tokens
	_, err := m.AuthTokens.DeleteMany(
		ctx,
		bson.M{
			"deviceId": token.UserDeviceId,
		},
	)
	if err != nil {
		m.Logger.Warn(ctx, "InsertUserToken DeleteMany", "error", err)
	}

	// Add the new one
	res, err := m.AuthTokens.InsertOne(
		ctx,
		token,
	)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	objId := res.InsertedID.(primitive.ObjectID)
	token.Id = objId.Hex()

	return token, nil
}

func (m *UserRepository) DropAll(ctx context.Context) error {
	err := m.UserCollection.Drop(ctx)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	err = m.AuthTokens.Drop(ctx)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	return nil
}
