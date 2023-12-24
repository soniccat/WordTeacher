package main

import (
	"tools"
	"tools/logger"
	"tools/mongowrapper"

	"github.com/alexedwards/scs/v2"

	"service_auth/internal/storage"
	"service_auth/internal/userauthtoken_generator"
)

type application struct {
	mongowrapper.MongoEnv
	logger                 *logger.Logger
	timeProvider           tools.TimeProvider
	sessionManager         *scs.SessionManager
	userRepository         *storage.UserRepository
	userAuthTokenGenerator userauthtoken_generator.UserAuthTokenGenerator
}

func createApplication(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	redisAddress string,
	mongoURI string,
	enableCredentials bool,
) (_ *application, err error) {
	app := &application{
		MongoEnv:       mongowrapper.NewMongoEnv(logger),
		logger:         logger,
		timeProvider:   timeProvider,
		sessionManager: tools.CreateSessionManager(redisAddress),
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

	app.userRepository = storage.NewUserRepository(app.logger, app.MongoWrapper.Client)
	app.userAuthTokenGenerator = userauthtoken_generator.NewUserAuthTokenGenerator(
		app.userRepository,
		app.sessionManager,
	)

	return app, nil
}

func (app *application) stop() {
	app.StopMongo()
}
