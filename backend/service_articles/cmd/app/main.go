package main

import (
	"context"
	"flag"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"os"
	"runtime/debug"
	"service_articles/internal/storage/headline_sources"
	"service_articles/internal/storage/headlines"
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
	serviceLogPath := flag.String("serviceLogPath", "/var/log", "service log file path")

	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4004, "HTTP server network port")
	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")

	flag.Parse()

	var serviceLogWriter io.Writer
	var err error

	if *isDebug {
		serviceLogWriter = os.Stdout
	} else {
		serviceLW, err := logger.NewLogWriter(*serviceLogPath, "service_", os.Stderr)
		if err != nil {
			fmt.Println("service NewLogWriter error: " + err.Error())
			return failCode
		}
		serviceLW.ScheduleRotation(context.Background())
		serviceLogWriter = serviceLW
	}

	logger := logger.New(serviceLogWriter, slog.Level(*minLogLevel))
	headlineStorage, err := headlines.New(
		logger,
		*mongoURI,
		*enableCredentials,
	)
	if err != nil {
		logger.ErrorWithError(context.Background(), err, "headline storage creation error")
		return failCode
	}

	headlineSourceStorage, err := headline_sources.New(
		logger,
		*mongoURI,
		*enableCredentials,
	)
	if err != nil {
		logger.ErrorWithError(context.Background(), err, "headline source storage creation error")
		return failCode
	}

	sessionManager := tools.CreateSessionManager(*redisAddress)
	app, err := createApplication(
		context.Background(),
		logger,
		&time_provider.TimeProvider{},
		sessionManager,
		session_validator.NewSessionManagerValidator(sessionManager),
		headlineStorage,
		headlineSourceStorage,
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

	// crawler
	crawler := app.createCrawler()

	go func() {
		crawler.Start(context.Background())
	}()

	serverURI := fmt.Sprintf("%s:%d", *serverAddr, *serverPort)
	srv := &http.Server{
		Addr:         serverURI,
		ErrorLog:     logger.AsLogLogger(slog.LevelError),
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
