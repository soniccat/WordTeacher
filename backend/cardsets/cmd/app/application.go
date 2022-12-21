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

func (app *application) GetLogger() *logger.Logger {
	return app.logger
}

func (app *application) SetMongoWrapper(mw *mongowrapper.MongoWrapper) {
	app.mongoWrapper = mw
}

func (app *application) GetMongoWrapper() *mongowrapper.MongoWrapper {
	return app.mongoWrapper
}

func (app *application) AllowStackTraces() bool {
	return app.logger.AllowStackTraces
}

func (app *application) NewHandlerError(code int, err error) *apphelpers.HandlerError {
	return apphelpers.NewHandlerError(code, err, app.AllowStackTraces())
}

type service struct {
	serverAddr string
	serverPort int
}

func (app *application) stop() {
	app.mongoWrapper.Stop()
}
