package main

import (
	"context"
	"flag"
	"fmt"
	"io"
	"log"
	"log/slog"
	"net/http"
	"os"
	"runtime/debug"
	"time"
	"tools"
	"tools/logger"
	"tools/time_provider"

	"models/session_validator"
)

const (
	successCode = 0
	failCode    = 1
)

func main() {
	os.Exit(run())
}

func run() int {
	// Define command-line flags
	isDebug := flag.Bool("debugMode", false, "Shows stack traces in logs")
	minLogLevel := flag.Int("logLevel", int(slog.LevelInfo), "minimum log level")
	serviceLogPath := flag.String("serviceLogPath", "/var/log/service_log", "service log file path")
	serverLogPath := flag.String("serverLogPath", "/var/log/server_log", "server log file path")

	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4003, "HTTP server network port")
	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")

	flag.Parse()

	var serverLogWriter io.Writer
	var serviceLogWriter io.Writer
	var err error

	if *isDebug {
		serverLogWriter = os.Stdout
		serviceLogWriter = os.Stdout
	} else {
		serverLogWriter, err = os.OpenFile(*serverLogPath, os.O_CREATE|os.O_WRONLY, os.ModeAppend)
		if err != nil {
			fmt.Println("server logfile open error: " + err.Error())
			return failCode
		}

		serviceLogWriter, err = os.OpenFile(*serviceLogPath, os.O_CREATE|os.O_WRONLY, os.ModeAppend)
		if err != nil {
			fmt.Println("service logfile open error: " + err.Error())
			return failCode
		}
	}

	logger := logger.New(serviceLogWriter, slog.Level(*minLogLevel))
	timeProvider := time_provider.TimeProvider{}
	sessionManager := tools.CreateSessionManager(*redisAddress)
	app, err := createApplication(
		context.Background(),
		logger,
		&timeProvider,
		sessionManager,
		*mongoURI,
		*enableCredentials,
		session_validator.NewSessionManagerValidator(sessionManager),
	)

	if err != nil {
		logger.ErrorWithError(context.Background(), err, "app creation error")
		return failCode
	}

	defer func() {
		app.stop()
	}()

	defer func() {
		msg := "panic"
		if r := recover(); r != nil {
			msg = fmt.Sprintf("panic: %v\n%s\n", r, string(debug.Stack()))
		}
		if err != nil {
			logger.ErrorWithError(context.Background(), err, msg)
		} else {
			logger.Error(context.Background(), msg)
		}
	}()

	// server
	serverLogger := log.New(serverLogWriter, "", log.LstdFlags)
	serverURI := fmt.Sprintf("%s:%d", *serverAddr, *serverPort)
	srv := &http.Server{
		Addr:         serverURI,
		ErrorLog:     serverLogger,
		Handler:      app.routes(),
		IdleTimeout:  time.Minute,
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
	}

	fmt.Printf("Starting server on %s", serverURI)
	err = srv.ListenAndServe()
	if err != nil {
		logger.ErrorWithError(context.Background(), err, "server error")
		return failCode
	}

	return successCode
}
