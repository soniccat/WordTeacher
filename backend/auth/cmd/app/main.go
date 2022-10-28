package main

import (
	"flag"
	"fmt"
	"models/apphelpers"
	"models/logger"
	"models/mongowrapper"
	"models/user"
	"net/http"
	"time"
)

func main() {
	// Define command-line flags
	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4000, "HTTP server network port")

	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017", "Database hostname url")
	//mongoDatabse := flag.String("mongoDatabase", "users", "Database name")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")

	flag.Parse()

	app, err := createApplication(redisAddress, mongoURI, enableCredentials)
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
	redisAddress *string,
	mongoURI *string,
	enableCredentials *bool,
) (*application, error) {
	app := &application{
		logger:         logger.New(),
		sessionManager: apphelpers.CreateSessionManager(redisAddress),
	}
	err := mongowrapper.SetupMongo(app, mongoURI, enableCredentials)
	if err != nil {
		return nil, err
	}

	usersDatabase := app.mongoWrapper.Client.Database(mongowrapper.MongoDatabaseUsers)
	app.userModel, err = user.NewUserModel(*app.mongoWrapper.Context, app.logger, usersDatabase)
	if err != nil {
		app.stop()
		return nil, err
	}

	return app, nil
}
