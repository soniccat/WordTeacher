package main

import (
	"github.com/alexedwards/scs/v2"
	"models/session_validator"
	"net/http"
	"service_cardsetsearch/internal/cardsetsearch"
	"tools"
	"tools/logger"
	"tools/mongowrapper"
)

type application struct {
	service                 service
	logger                  *logger.Logger
	sessionManager          *scs.SessionManager
	mongoWrapper            *mongowrapper.MongoWrapper
	cardSetSearchRepository *cardset.Repository
	sessionValidator        session_validator.SessionValidator
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

func (app *application) NewHandlerError(code int, err error) *tools.HandlerError {
	return tools.NewHandlerError(err, code, app.AllowStackTraces())
}

func (app *application) SetHandlerError(w http.ResponseWriter, err *tools.HandlerError) {
	tools.SetHandlerError(w, err, app.GetLogger())
}

func (app *application) SetError(w http.ResponseWriter, outErr error, code int) {
	tools.SetError(w, outErr, code, app.GetLogger())
}

func (app *application) WriteResponse(w http.ResponseWriter, response interface{}) {
	tools.WriteResponse(w, response, app.GetLogger())
}

type service struct {
	serverAddr string
	serverPort int
}

func (app *application) stop() {
	app.mongoWrapper.Stop()
}