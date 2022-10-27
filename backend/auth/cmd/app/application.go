package main

import (
	"auth/cmd/mongowrapper"
	"auth/cmd/userauthtoken"
	"auth/cmd/usernetwork"
	"context"
	"github.com/alexedwards/scs/v2"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/writeconcern"
	"log"
)

type application struct {
	service        service
	logger         *logger
	sessionManager *scs.SessionManager
	mongoWrapper   *mongowrapper.MongoWrapper
	userModel      *UserModel
}

type service struct {
	serverAddr string
	serverPort int
}

type logger struct {
	error *log.Logger
	info  *log.Logger
}

func (app *application) setupMongo(mongoURI *string, enableCredentials *bool) error {
	mongoWrapper, err := mongowrapper.New(mongoURI, enableCredentials)
	if err != nil {
		app.logger.error.Printf("createMongoWrapper failed: %s", err.Error())
		return err
	}

	if err = mongoWrapper.Connect(); err != nil {
		app.logger.error.Printf("mongoWrapper.connect() failed: %s", err.Error())
		return err
	}

	app.userModel = &UserModel{
		app.logger,
		mongoWrapper.Client.Database(MongoDatabaseUsers).
			Collection(
				MongoCollectionUserCounter,
				&options.CollectionOptions{
					WriteConcern: writeconcern.New(writeconcern.WMajority()),
				},
			),
		mongoWrapper.Client.Database(MongoDatabaseUsers).Collection(MongoCollectionUsers),
		mongoWrapper.Client.Database(MongoDatabaseUsers).Collection(MongoCollectionAuthTokens),
	}
	app.mongoWrapper = mongoWrapper

	return nil
}

func (app *application) stop() {
	app.mongoWrapper.Stop()
}

func (app *application) InsertUserAuthToken(
	context context.Context,
	userMongoId *primitive.ObjectID,
	networkType usernetwork.UserNetworkType,
	deviceId string,
) (*userauthtoken.UserAuthToken, error) {
	token, err := app.userModel.InsertUserAuthToken(
		context,
		userMongoId,
		networkType,
		deviceId,
	)
	if err != nil {
		return nil, err
	}

	token.SaveAsSession(context, app.sessionManager)
	return token, nil
}
