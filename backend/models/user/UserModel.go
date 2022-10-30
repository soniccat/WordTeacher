package user

import (
	"context"
	"errors"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/writeconcern"
	"models/logger"
	"models/mongowrapper"
	"models/userauthtoken"
	"models/usernetwork"
)

const MongoUserCounter = "count"

// TODO: move in auth module
type UserModel struct {
	Logger            *logger.Logger
	CounterCollection *mongo.Collection
	UserCollection    *mongo.Collection
	AuthTokens        *mongo.Collection
}

func NewUserModel(context context.Context, logger *logger.Logger, usersDatabase *mongo.Database) (*UserModel, error) {
	model := &UserModel{
		Logger: logger,
		CounterCollection: usersDatabase.
			Collection(
				mongowrapper.MongoCollectionUserCounter,
				&options.CollectionOptions{
					WriteConcern: writeconcern.New(writeconcern.WMajority()),
				},
			),
		UserCollection: usersDatabase.Collection(mongowrapper.MongoCollectionUsers),
		AuthTokens:     usersDatabase.Collection(mongowrapper.MongoCollectionAuthTokens),
	}
	err := model.prepare(context)
	if err != nil {
		return nil, err
	}

	return model, nil
}

func (m *UserModel) prepare(context context.Context) error {
	var counter = UserCounter{}
	err := m.CounterCollection.FindOne(context, bson.M{}).Decode(&counter)

	if err == mongo.ErrNoDocuments {
		if _, err = m.CounterCollection.InsertOne(context, bson.M{MongoUserCounter: uint64(1)}); err != nil {
			return err
		}

		return nil
	}

	return err
}

func (m *UserModel) FindGoogleUser(context context.Context, googleUserId *string) (*User, error) {
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

func (m *UserModel) InsertUser(context context.Context, user *User) (*User, error) {
	userCounter, err := m.GetNewUserCounter(context)
	if err != nil {
		return nil, err
	}

	user.Counter = userCounter
	res, err := m.UserCollection.InsertOne(context, user)
	if err != nil {
		return nil, err
	}

	objId, ok := res.InsertedID.(primitive.ObjectID)
	if !ok {
		return nil, errors.New("InsertUser, InsertedID cast")
	}

	var newUser = *user
	newUser.ID = objId

	return &newUser, nil
}

func (m *UserModel) GetNewUserCounter(context context.Context) (uint64, error) {
	var counter = UserCounter{}
	err := m.CounterCollection.FindOneAndUpdate(
		context,
		bson.M{},
		bson.M{"$inc": bson.M{MongoUserCounter: 1}},
		options.FindOneAndUpdate().SetReturnDocument(options.After),
	).Decode(&counter)
	if err != nil {
		return 0, err
	}

	return counter.Count, nil
}

// TODO: move in auth module
func (m *UserModel) GenerateUserAuthToken(
	context context.Context,
	userId *primitive.ObjectID,
	networkType usernetwork.UserNetworkType,
	deviceId string,
) (*userauthtoken.UserAuthToken, error) {
	token, err := userauthtoken.Generate(userId, networkType, deviceId)
	if err != nil {
		return nil, err
	}

	return m.insertUserAuthToken(context, token)
}

// TODO: move in auth module
func (m *UserModel) insertUserAuthToken(
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
		m.Logger.Error.Printf("InsertUserToken DeleteMany error %f", err.Error())
	}

	// Add the new one
	res, err := m.AuthTokens.InsertOne(
		context,
		token,
	)
	if err != nil {
		return nil, err
	}

	objId, ok := res.InsertedID.(primitive.ObjectID)
	if !ok {
		return nil, errors.New("GenerateUserAuthToken, InsertedID cast")
	}

	token.ID = &objId
	return token, nil
}
