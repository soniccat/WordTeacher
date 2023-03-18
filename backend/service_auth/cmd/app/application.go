package main

import (
	"context"
	"github.com/alexedwards/scs/v2"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models"
	"service_auth/internal"
	appUsearAuthToken "service_auth/internal/userauthtoken"
	"tools/logger"
	"tools/mongowrapper"
)

type application struct {
	service        service
	logger         *logger.Logger
	sessionManager *scs.SessionManager
	mongoWrapper   *mongowrapper.MongoWrapper
	userModel      *internal.UserRepository
}

func (a *application) GetLogger() *logger.Logger {
	return a.logger
}

func (a *application) SetMongoWrapper(mw *mongowrapper.MongoWrapper) {
	a.mongoWrapper = mw
}

func (a *application) GetMongoWrapper() *mongowrapper.MongoWrapper {
	return a.mongoWrapper
}

type service struct {
	serverAddr string
	serverPort int
}

func (app *application) stop() {
	app.mongoWrapper.Stop()
}

func (app *application) GenerateUserAuthToken(
	context context.Context,
	userMongoId *primitive.ObjectID,
	networkType models.UserNetworkType,
	deviceType string,
	deviceId string,
) (*models.UserAuthToken, error) {
	token, err := app.userModel.GenerateUserAuthToken(
		context,
		userMongoId,
		networkType,
		deviceType,
		deviceId,
	)
	if err != nil {
		return nil, err
	}

	appUsearAuthToken.SaveUserAuthTokenAsSession(token, context, app.sessionManager)
	return token, nil
}
