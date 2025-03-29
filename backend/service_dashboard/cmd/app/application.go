package main

import (
	"context"
	"tools"
	"tools/logger"

	"github.com/alexedwards/scs/v2"

	"models/session_validator"
	"service_dashboard/internal/storage/cardsets"
	"service_dashboard/internal/storage/headlines"
)

type application struct {
	Context          context.Context
	cancelF          context.CancelFunc
	logger           *logger.Logger
	timeProvider     tools.TimeProvider
	sessionManager   *scs.SessionManager
	headlineStorage  *headlines.Storage
	sessionValidator session_validator.SessionValidator
	cardsetStorage   *cardsets.Storage
}

func createApplication(
	ctx context.Context,
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionManager *scs.SessionManager,
	sessionValidator session_validator.SessionValidator,
	headlineStorage *headlines.Storage,
	cardsetStorage *cardsets.Storage,
) (_ *application, err error) {
	appCtx, cancelF := context.WithCancel(ctx)
	app := &application{
		Context:          appCtx,
		cancelF:          cancelF,
		logger:           logger,
		timeProvider:     timeProvider,
		sessionManager:   sessionManager,
		headlineStorage:  headlineStorage,
		sessionValidator: sessionValidator,
		cardsetStorage:   cardsetStorage,
	}

	defer func() {
		if err != nil {
			app.stop()
		}
	}()

	return app, nil
}

func (app *application) stop() {
	app.cancelF()
}

func (app *application) StartPullingArticles() {
	app.headlineStorage.StartPulling(app.Context)
}

func (app *application) StartPullingCardSets() {
	app.cardsetStorage.StartPulling(app.Context)
}
