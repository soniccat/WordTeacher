package main

import (
	"context"
	"tools"

	"github.com/alexedwards/scs/v2"

	"models/session_validator"
	"service_dict/internal/wiktionary"
	"tools/logger"
	"tools/mongowrapper"
)

type application struct {
	mongowrapper.MongoEnv
	logger               *logger.Logger
	timeProvider         tools.TimeProvider
	sessionManager       *scs.SessionManager
	wiktionaryRepository wiktionary.Contract
	sessionValidator     session_validator.SessionValidator
}

func createApplication(
	ctx context.Context,
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionManager *scs.SessionManager,
	mongoURI string,
	enableCredentials bool,
	sessionValidator session_validator.SessionValidator,
) (_ *application, err error) {
	app := &application{
		MongoEnv:         mongowrapper.NewMongoEnv(logger),
		logger:           logger,
		timeProvider:     timeProvider,
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

	app.wiktionaryRepository = wiktionary.New(app.logger, app.MongoWrapper.Client)
	err = app.wiktionaryRepository.CreateIndexIfNeeded(ctx)
	if err != nil {
		return nil, err
	}

	return app, nil
}

func (app *application) stop() {
	app.StopMongo()
}
