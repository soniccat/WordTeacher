package main

import (
	"context"
	"flag"
	"fmt"
	"net/http"
	"runtime/debug"
	"time"
	"tools"
	"tools/logger"

	"models/session_validator"
)

func main() {
	// Define command-line flags
	isDebug := flag.Bool("debugMode", false, "Shows stack traces in logs")
	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4003, "HTTP server network port")

	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")

	flag.Parse()

	logger := logger.New(*isDebug)
	sessionManager := tools.CreateSessionManager(*redisAddress)
	app, err := createApplication(
		context.Background(),
		logger,
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

	// server
	serverURI := fmt.Sprintf("%s:%d", *serverAddr, *serverPort)
	srv := &http.Server{
		Addr:         serverURI,
		ErrorLog:     logger.Error,
		Handler:      app.routes(),
		IdleTimeout:  time.Minute,
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
	}

	logger.Info.Printf("Starting server on %s", serverURI)
	err = srv.ListenAndServe()
	logger.Error.Fatal(err)
}