package main

import (
	"github.com/alexedwards/scs/v2"

	"models/session_validator"
	"service_cardsets/internal/storage"
	"tools/logger"
)

type application struct {
	logger            *logger.Logger
	sessionManager    *scs.SessionManager
	cardSetRepository *storage.Storage
	sessionValidator  session_validator.SessionValidator
}

func createApplication(
	logger *logger.Logger,
	sessionManager *scs.SessionManager,
	sessionValidator session_validator.SessionValidator,
	cardSetRepository *storage.Storage,
) (_ *application, err error) {
	app := &application{
		logger:            logger,
		sessionManager:    sessionManager,
		cardSetRepository: cardSetRepository,
		sessionValidator:  sessionValidator,
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
