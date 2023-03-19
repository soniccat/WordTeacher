package main

import (
	"flag"
	"fmt"
	"github.com/alexedwards/scs/v2"
	"models/helpers"
	"net/http"
	"runtime/debug"
	"service_cardsets/internal/cardset"
	"time"
	"tools"
	"tools/logger"
	"tools/mongowrapper"
)

func main() {
	// Define command-line flags
	isDebug := flag.Bool("debugMode", false, "Shows stack traces in logs")
	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4001, "HTTP server network port")

	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")

	flag.Parse()

	sessionManager := tools.CreateSessionManager(*redisAddress)
	app, err := createApplication(
		*isDebug,
		sessionManager,
		*mongoURI,
		*enableCredentials,
		helpers.NewSessionManagerValidator(sessionManager),
	)
	defer func() {
		app.stop()
	}()

	if err != nil {
		return
	}

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
	isDebug bool,
	sessionManager *scs.SessionManager,
	mongoURI string,
	enableCredentials bool,
	sessionValidator helpers.SessionValidator,
) (*application, error) {
	app := &application{
		logger:           logger.New(isDebug),
		sessionManager:   sessionManager,
		sessionValidator: sessionValidator,
	}
	err := mongowrapper.SetupMongo(app, mongoURI, enableCredentials)
	if err != nil {
		return nil, err
	}

	app.cardSetRepository = cardset.New(app.logger, app.mongoWrapper.Client)

	return app, nil
}
