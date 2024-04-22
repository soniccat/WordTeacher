package main

import (
	"context"
	"tools"

	"github.com/alexedwards/scs/v2"

	"models/session_validator"
	"service_cardsetsearch/internal/cardsetpull_worker"
	"service_cardsetsearch/internal/cardsets_client"
	"service_cardsetsearch/internal/storage"
	"tools/logger"
	"tools/mongowrapper"
	"tools/time_provider"
)

type application struct {
	mongowrapper.MongoEnv
	ctx                     context.Context
	logger                  *logger.Logger
	timeProvider            tools.TimeProvider
	sessionManager          *scs.SessionManager
	cardSetSearchRepository *storage.Repository
	sessionValidator        session_validator.SessionValidator
	cardSetsClient          cardsets_client.Contract
}

func createApplication(
	ctx context.Context,
	logger *logger.Logger,
	redisAddress string,
	mongoURI string,
	enableCredentials bool,
	cardSetsClient cardsets_client.Contract,
) (_ *application, err error) {
	sessionManager := tools.CreateSessionManager(redisAddress)
	sessionValidator := session_validator.NewSessionManagerValidator(sessionManager)

	app := &application{
		MongoEnv:         mongowrapper.NewMongoEnv(logger),
		ctx:              ctx,
		logger:           logger,
		timeProvider:     &time_provider.TimeProvider{},
		sessionManager:   sessionManager,
		sessionValidator: sessionValidator,
		cardSetsClient:   cardSetsClient,
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

	app.cardSetSearchRepository = storage.New(app.logger, app.MongoWrapper.Client)
	err = app.cardSetSearchRepository.CreateTextIndexIfNeeded(ctx)
	if err != nil {
		return nil, err
	}

	return app, nil
}

func (app *application) startCardSetPullWorker() error {
	// worker
	worker := cardsetpull_worker.NewCardSetPullWorker(app.logger, app.cardSetsClient, app.cardSetSearchRepository)
	return worker.Start(app.ctx)
}

func (app *application) stop() {
	app.StopMongo()
}
