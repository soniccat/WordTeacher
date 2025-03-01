package main

import (
	"service_articles/internal/storage"
	"tools"

	"github.com/alexedwards/scs/v2"

	"models/session_validator"
	"tools/logger"
)

type application struct {
	logger             *logger.Logger
	timeProvider       tools.TimeProvider
	sessionManager     *scs.SessionManager
	headlineRepository *storage.Storage
	sessionValidator   session_validator.SessionValidator
}

func createApplication(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionManager *scs.SessionManager,
	sessionValidator session_validator.SessionValidator,
	headlineRepository *storage.Storage,
) (_ *application, err error) {
	app := &application{
		logger:             logger,
		timeProvider:       timeProvider,
		sessionManager:     sessionManager,
		headlineRepository: headlineRepository,
		sessionValidator:   sessionValidator,
	}

	defer func() {
		if err != nil {
			app.stop()
		}
	}()

	return app, nil
}

func (app *application) stop() {
}
