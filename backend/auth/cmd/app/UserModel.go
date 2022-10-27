package main

import (
	"auth/cmd/userauthtoken"
	"auth/cmd/usernetwork"
	"context"
	"errors"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

const MongoUserCounter = "count"

type UserModel struct {
	logger            *logger
	counterCollection *mongo.Collection
	userCollection    *mongo.Collection
	authTokens        *mongo.Collection
}

func (m *UserModel) prepare(context context.Context) error {
	var counter = UserCounter{}
	err := m.counterCollection.FindOne(context, bson.M{}).Decode(&counter)

	if err == mongo.ErrNoDocuments {
		if _, err = m.counterCollection.InsertOne(context, bson.M{MongoUserCounter: uint64(1)}); err != nil {
			return err
		}

		return nil
	}

	return err
}

func (m *UserModel) FindGoogleUser(context context.Context, googleUserId *string) (*User, error) {
	var user = User{}

	err := m.userCollection.FindOne(
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
	res, err := m.userCollection.InsertOne(context, user)
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
	err := m.counterCollection.FindOneAndUpdate(
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

func (m *UserModel) InsertUserAuthToken(
	context context.Context,
	userId *primitive.ObjectID,
	networkType usernetwork.UserNetworkType,
	deviceId string,
) (*userauthtoken.UserAuthToken, error) {
	token, err := userauthtoken.New(userId, networkType, deviceId)
	if err != nil {
		return nil, err
	}

	return m.insertUserAuthToken(context, token)
}

func (m *UserModel) insertUserAuthToken(
	context context.Context,
	token *userauthtoken.UserAuthToken,
) (*userauthtoken.UserAuthToken, error) {
	// Remove stale auth tokens
	_, err := m.authTokens.DeleteMany(
		context,
		bson.M{
			"deviceId": token.DeviceId,
		},
	)
	if err != nil {
		m.logger.error.Printf("InsertUserToken DeleteMany error %f", err.Error())
	}

	// Add the new one
	res, err := m.authTokens.InsertOne(
		context,
		token,
	)
	if err != nil {
		return nil, err
	}

	objId, ok := res.InsertedID.(primitive.ObjectID)
	if !ok {
		return nil, errors.New("InsertUserAuthToken, InsertedID cast")
	}

	token.ID = &objId
	return token, nil
}
