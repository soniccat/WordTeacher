package main

import (
	"service_articles/internal/storage/headline_sources"
	"service_articles/internal/storage/headlines"
	"tools"

	"github.com/alexedwards/scs/v2"

	"models/session_validator"
	"tools/logger"
)

type application struct {
	logger                   *logger.Logger
	timeProvider             tools.TimeProvider
	sessionManager           *scs.SessionManager
	headlineRepository       *headlines.Storage
	headlineSourceRepository *headline_sources.Storage
	sessionValidator         session_validator.SessionValidator
}

func createApplication(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionManager *scs.SessionManager,
	sessionValidator session_validator.SessionValidator,
	headlineRepository *headlines.Storage,
	headlineSourceRepository *headline_sources.Storage,
) (_ *application, err error) {
	app := &application{
		logger:                   logger,
		timeProvider:             timeProvider,
		sessionManager:           sessionManager,
		headlineRepository:       headlineRepository,
		headlineSourceRepository: headlineSourceRepository,
		sessionValidator:         sessionValidator,
	}

	defer func() {
		if err != nil {
			app.stop()
		}
	}()

	return app, nil
}

func (app *application) stop() {
	app.headlineRepository.StopMongo()
}
