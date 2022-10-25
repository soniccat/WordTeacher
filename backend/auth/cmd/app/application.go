package main

import (
	"github.com/alexedwards/scs/v2"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/writeconcern"
	"log"
)

type application struct {
	service        service
	logger         *logger
	sessionManager *scs.SessionManager
	mongoWrapper   *MongoWrapper
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
	mongoWrapper, err := createMongoWrapper(mongoURI, enableCredentials)
	if err != nil {
		app.logger.error.Printf("createMongoWrapper failed: %s", err.Error())
		return err
	}

	if err = mongoWrapper.connect(); err != nil {
		app.logger.error.Printf("mongoWrapper.connect() failed: %s", err.Error())
		return err
	}

	app.userModel = &UserModel{
		mongoWrapper.client.Database(MongoDatabaseUsers).
			Collection(
				MongoCollectionUserCounter,
				&options.CollectionOptions{
					WriteConcern: writeconcern.New(writeconcern.WMajority()),
				},
			),
		mongoWrapper.client.Database(MongoDatabaseUsers).Collection(MongoCollectionUsers),
		mongoWrapper.client.Database(MongoDatabaseUsers).Collection(MongoCollectionAuthTokens),
	}
	app.mongoWrapper = mongoWrapper

	return nil
}

func (app *application) stop() {
	app.mongoWrapper.stop()
}
