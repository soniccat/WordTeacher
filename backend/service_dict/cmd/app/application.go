package main

import (
	"context"
	"service_dict/internal/wiktionary/repository"
	"service_dict/internal/wiktionary/repository_v2"
	"tools"

	"github.com/alexedwards/scs/v2"

	"models/session_validator"
	"tools/logger"
	"tools/mongowrapper"
)

type application struct {
	mongowrapper.MongoEnv
	logger                 *logger.Logger
	timeProvider           tools.TimeProvider
	sessionManager         *scs.SessionManager
	wiktionaryRepositoryV1 repository.Repository
	wiktionaryRepositoryV2 repository_v2.Repository
	sessionValidator       session_validator.SessionValidator
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

	app.wiktionaryRepositoryV1 = repository.New(app.logger, app.MongoWrapper)
	err = app.wiktionaryRepositoryV1.CreateIndexIfNeeded()
	if err != nil {
		return nil, err
	}

	app.wiktionaryRepositoryV2 = repository_v2.New(app.logger, app.MongoWrapper)
	err = app.wiktionaryRepositoryV2.CreateIndexIfNeeded()
	if err != nil {
		return nil, err
	}

	err = app.wiktionaryRepositoryV2.CreateWordExamplesTextIndexIfNeeded(ctx)
	if err != nil {
		return nil, err
	}

	return app, nil
}

func (app *application) stop() {
	app.StopMongo()
}
