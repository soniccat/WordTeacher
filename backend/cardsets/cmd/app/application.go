package main

import (
	"github.com/alexedwards/scs/v2"
	"models/apphelpers"
	"models/cardset"
	"models/logger"
	"models/mongowrapper"
)

type application struct {
	service        service
	logger         *logger.Logger
	sessionManager *scs.SessionManager
	mongoWrapper   *mongowrapper.MongoWrapper
	cardSetModel   *cardset.CardSetModel
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

func (a *application) AllowStackTraces() bool {
	return a.logger.AllowStackTraces
}

func (a *application) NewHandlerError(code int, err error) *apphelpers.HandlerError {
	return apphelpers.NewHandlerError(code, err, a.AllowStackTraces())
}

type service struct {
	serverAddr string
	serverPort int
}

func (app *application) stop() {
	app.mongoWrapper.Stop()
}
