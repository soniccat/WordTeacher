package main

import (
	"github.com/alexedwards/scs/v2"

	"models/session_validator"
	"service_cardsets/internal/storage"
	"tools/logger"
	"tools/mongowrapper"
)

type application struct {
	mongowrapper.MongoEnv
	logger            *logger.Logger
	sessionManager    *scs.SessionManager
	cardSetRepository *storage.Repository
	sessionValidator  session_validator.SessionValidator
}

func createApplication(
	logger *logger.Logger,
	sessionManager *scs.SessionManager,
	mongoURI string,
	enableCredentials bool,
	sessionValidator session_validator.SessionValidator,
) (_ *application, err error) {
	app := &application{
		MongoEnv:         mongowrapper.NewMongoEnv(logger),
		logger:           logger,
		sessionManager:   sessionManager,
		sessionValidator: sessionValidator,
	}

	defer func() {
		if err != nil {
			app.stop()
		}
	}()

	err = app.SetupMongo(mongoURI, enableCredentials)
	if err != nil {
		return nil, err
	}

	app.cardSetRepository = storage.New(app.logger, app.MongoWrapper.Client)

	return app, nil
}

func (app *application) stop() {
	app.StopMongo()
}
