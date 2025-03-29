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
	articlesgrpc "service_articles/pkg/grpc/service_articles/api"
	cardsetsgrpc "service_cardsets/pkg/grpc/service_cardsets/api"
	"service_dashboard/internal/client/articles"
	"service_dashboard/internal/client/cardsets"
	cardsetStorage "service_dashboard/internal/storage/cardsets"
	"service_dashboard/internal/storage/headlines"
	"time"
	"tools"
	"tools/logger"
	"tools/time_provider"

	"models/session_validator"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
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
	// mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	articlesGRPCAddress := flag.String("articlesGRPCAddress", "localhost:5004", "get headlines")
	cardSetsGRPCAddress := flag.String("cardSetsGRPCAddress", "localhost:5001", "get new cardsets")
	// enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")

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

	// grpc
	articlesGRPCConnection, err := grpc.Dial(*articlesGRPCAddress, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatalf("cardSetGRPCConnection did not connect: %v", err)
	}
	defer articlesGRPCConnection.Close()
	articlesGrpcClient := articlesgrpc.NewHeadlinesClient(articlesGRPCConnection)
	ariclesClient := articles.New(logger, articlesGrpcClient)

	cardSetsGRPCConnection, err := grpc.Dial(*cardSetsGRPCAddress, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatalf("cardSetGRPCConnection did not connect: %v", err)
	}
	defer cardSetsGRPCConnection.Close()
	cardSetsGrpcClient := cardsetsgrpc.NewCardSetsClient(cardSetsGRPCConnection)
	cardSetsClient := cardsets.New(logger, cardSetsGrpcClient)

	// storages
	headlineStorage := headlines.New(
		logger,
		&ariclesClient,
	)
	if err != nil {
		logger.ErrorWithError(context.Background(), err, "headline storage creation error")
		return failCode
	}

	cardSetsStorage := cardsetStorage.New(logger, &cardSetsClient)

	sessionManager := tools.CreateSessionManager(*redisAddress)
	app, err := createApplication(
		context.Background(),
		logger,
		&time_provider.TimeProvider{},
		sessionManager,
		session_validator.NewSessionManagerValidator(sessionManager),
		&headlineStorage,
		&cardSetsStorage,
	)
	if err != nil {
		logger.ErrorWithError(context.Background(), err, "app creation error")
		return failCode
	}
	app.StartPullingArticles()
	app.StartPullingCardSets()

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
