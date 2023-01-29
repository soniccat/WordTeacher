package main

import (
	"github.com/alexedwards/scs/v2"
	"models/apphelpers"
	"models/cardset"
	"models/logger"
	"models/mongowrapper"
	"models/user"
	"net/http"
)

type application struct {
	service           service
	logger            *logger.Logger
	sessionManager    *scs.SessionManager
	mongoWrapper      *mongowrapper.MongoWrapper
	cardSetRepository *cardset.Repository
	sessionValidator  user.SessionValidator
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
	return apphelpers.NewHandlerError(err, code, app.AllowStackTraces())
}

func (app *application) SetHandlerError(w http.ResponseWriter, err *apphelpers.HandlerError) {
	apphelpers.SetHandlerError(w, err, app.GetLogger())
}

func (app *application) SetError(w http.ResponseWriter, outErr error, code int) {
	apphelpers.SetError(w, outErr, code, app.GetLogger())
}

func (app *application) WriteResponse(w http.ResponseWriter, response interface{}) {
	apphelpers.WriteResponse(w, response, app.GetLogger())
}

type service struct {
	serverAddr string
	serverPort int
}

func (app *application) stop() {
	app.mongoWrapper.Stop()
}
