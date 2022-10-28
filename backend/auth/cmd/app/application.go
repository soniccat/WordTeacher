package main

import (
	"context"
	"github.com/alexedwards/scs/v2"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/logger"
	"models/mongowrapper"
	"models/user"
	"models/userauthtoken"
	"models/usernetwork"
)

type application struct {
	service        service
	logger         *logger.Logger
	sessionManager *scs.SessionManager
	mongoWrapper   *mongowrapper.MongoWrapper
	userModel      *user.UserModel
}

func (a *application) GetLogger() *logger.Logger {
	return a.logger
}

func (a *application) SetMongoWrapper(mw *mongowrapper.MongoWrapper) {
	a.mongoWrapper = mw
}

type service struct {
	serverAddr string
	serverPort int
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
