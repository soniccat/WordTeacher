package main

import (
	"tools"
	"tools/logger"
	"tools/mongowrapper"

	"service_auth/internal/service_models"
	"service_auth/internal/services/authorizer"
	"service_auth/internal/services/token_refresher"
	"service_auth/internal/services/user_provider"
	"service_auth/internal/services/userauthtoken_generator"
	"service_auth/internal/storage"

	"github.com/alexedwards/scs/v2"
)

type application struct {
	mongowrapper.MongoEnv
	logger         *logger.Logger
	timeProvider   tools.TimeProvider
	sessionManager *scs.SessionManager
	// userRepository *storage.UserRepository
	authorizer     *authorizer.Service
	tokenRefresher *token_refresher.Service
}

func createApplication(
	logger *logger.Logger,
	config service_models.Configs,
	timeProvider tools.TimeProvider,
	redisAddress string,
	mongoURI string,
	enableCredentials bool,
) (_ *application, err error) {
	sessionManager := tools.CreateSessionManager(redisAddress)
	app := &application{
		MongoEnv:       mongowrapper.NewMongoEnv(logger),
		logger:         logger,
		timeProvider:   timeProvider,
		sessionManager: sessionManager,
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

	userRepository := storage.NewUserRepository(app.logger, app.MongoWrapper.Client)
	userAuthTokenGenerator := userauthtoken_generator.NewUserAuthTokenGenerator(
		userRepository,
		sessionManager,
	)
	userResolver := user_provider.New(config.GoogleConfig, config.VKIDConfig, userRepository)

	app.authorizer = authorizer.New(
		userResolver,
		userRepository,
		userAuthTokenGenerator,
	)
	app.tokenRefresher = token_refresher.New(
		userAuthTokenGenerator,
		sessionManager,
	)

	return app, nil
}

func (app *application) stop() {
	app.StopMongo()
}
