package main

import (
	"context"

	"github.com/alexedwards/scs/v2"

	"models/session_validator"
	"service_cardsetsearch/internal/cardsets_client"
	"service_cardsetsearch/internal/storage"
	"tools/logger"
	"tools/mongowrapper"
)

type application struct {
	mongowrapper.MongoApp
	logger                  *logger.Logger
	sessionManager          *scs.SessionManager
	cardSetSearchRepository *storage.Repository
	sessionValidator        session_validator.SessionValidator
	cardSetsClient          cardsets_client.Contract
}

func createApplication(
	ctx context.Context,
	logger *logger.Logger,
	sessionManager *scs.SessionManager,
	mongoURI string,
	enableCredentials bool,
	sessionValidator session_validator.SessionValidator,
	cardSetsClient cardsets_client.Contract,
) (_ *application, err error) {
	app := &application{
		MongoApp:         mongowrapper.NewMongoApp(logger),
		logger:           logger,
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

func (app *application) stop() {
	app.StopMongo()
}
