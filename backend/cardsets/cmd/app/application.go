package main

import (
	"github.com/alexedwards/scs/v2"
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

type service struct {
	serverAddr string
	serverPort int
}

func (app *application) stop() {
	app.mongoWrapper.Stop()
}
