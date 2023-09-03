package main

import (
	"context"
	"flag"
	"fmt"
	"models/session_validator"
	"net/http"
	"runtime/debug"
	"service_cardsetsearch/internal/cardsetsearch"
	"time"
	"tools"
	"tools/logger"
	"tools/mongowrapper"

	"github.com/alexedwards/scs/v2"
)

func main() {
	// Define command-line flags
	isDebug := flag.Bool("debugMode", false, "Shows stack traces in logs")
	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4002, "HTTP server network port")

	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")

	flag.Parse()

	ctx := context.Background()

	sessionManager := tools.CreateSessionManager(*redisAddress)
	app, err := createApplication(
		ctx,
		*isDebug,
		sessionManager,
		*mongoURI,
		*enableCredentials,
		session_validator.NewSessionManagerValidator(sessionManager),
	)

	if err != nil {
		fmt.Println("app creation error: " + err.Error())
		return
	}

	defer func() {
		app.stop()
	}()

	defer func() {
		if r := recover(); r != nil {
			fmt.Println("stacktrace from panic: \n" + string(debug.Stack()))
		}
	}()

	// Initialize a new http.Server struct.
	serverURI := fmt.Sprintf("%s:%d", *serverAddr, *serverPort)
	srv := &http.Server{
		Addr:         serverURI,
		ErrorLog:     app.logger.Error,
		Handler:      app.routes(),
		IdleTimeout:  time.Minute,
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
	}

	app.logger.Info.Printf("Starting server on %s", serverURI)
	err = srv.ListenAndServe()
	app.logger.Error.Fatal(err)
}

func createApplication(
	ctx context.Context,
	isDebug bool,
	sessionManager *scs.SessionManager,
	mongoURI string,
	enableCredentials bool,
	sessionValidator session_validator.SessionValidator,
) (_ *application, err error) {
	app := &application{
		logger:           logger.New(isDebug),
		sessionManager:   sessionManager,
		sessionValidator: sessionValidator,
	}

	defer func() {
		if err != nil {
			app.stop()
		}
	}()

	err = mongowrapper.SetupMongo(app, mongoURI, enableCredentials)
	if err != nil {
		return nil, err
	}

	app.cardSetSearchRepository = cardsetsearch.New(app.logger, app.mongoWrapper.Client)
	err = app.cardSetSearchRepository.CreateTextIndexIfNeeded(ctx)
	if err != nil {
		return nil, err
	}

	return app, nil
}
