package main

import (
	"flag"
	"fmt"
	"models/apphelpers"
	"models/card"
	"models/cardset"
	"models/logger"
	"models/mongowrapper"
	"net/http"
	"time"
)

func main() {
	// Define command-line flags
	isDebug := flag.Bool("debugMode", false, "Shows stack traces in logs")
	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4001, "HTTP server network port")

	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")

	flag.Parse()

	app, err := createApplication(*isDebug, redisAddress, mongoURI, enableCredentials)
	defer func() {
		app.stop()
	}()

	if err != nil {
		return
	}

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
	redisAddress *string,
	mongoURI *string,
	enableCredentials *bool,
) (*application, error) {
	app := &application{
		logger:         logger.New(isDebug),
		sessionManager: apphelpers.CreateSessionManager(redisAddress),
	}
	err := mongowrapper.SetupMongo(app, mongoURI, enableCredentials)
	if err != nil {
		return nil, err
	}

	cardSetDatabase := app.mongoWrapper.Client.Database(mongowrapper.MongoDatabaseCardSets)
	cardModel := card.New(app.logger, cardSetDatabase)
	app.cardSetModel = cardset.New(app.logger, app.mongoWrapper.Client, cardSetDatabase, cardModel)

	return app, nil
}
