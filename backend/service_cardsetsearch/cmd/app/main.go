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

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"

	cardsetsgrpc "service_cardsets/pkg/grpc/service_cardsets/api"
	"service_cardsetsearch/internal/cardsets_client"
	"tools/logger"
)

const (
	successCode = 0
	failCode    = 1
)

func main() {
	os.Exit(run())
}

func run() int {
	// flags
	isDebug := flag.Bool("debugMode", false, "Shows stack traces in logs")
	minLogLevel := flag.Int("logLevel", int(slog.LevelInfo), "minimum log level")
	serviceLogPath := flag.String("serviceLogPath", "/var/log", "service log file path")
	serverLogPath := flag.String("serverLogPath", "/var/log", "server log file path")

	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4002, "HTTP server network port")
	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	cardSetsGRPCAddress := flag.String("cardSetsGPRCAddress", "localhost:5001", "get new cardsets")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")

	flag.Parse()

	var serverLogWriter io.Writer
	var serviceLogWriter io.Writer
	var err error

	if *isDebug {
		serverLogWriter = os.Stdout
		serviceLogWriter = os.Stdout
	} else {
		serverLW, err := logger.NewLogWriter(*serverLogPath, "server_", os.Stderr)
		if err != nil {
			fmt.Println("server NewLogWriter error: " + err.Error())
			return failCode
		}
		serverLW.ScheduleRotation(context.Background())
		serverLogWriter = serverLW

		serviceLW, err := logger.NewLogWriter(*serviceLogPath, "service_", os.Stderr)
		if err != nil {
			fmt.Println("service NewLogWriter error: " + err.Error())
			return failCode
		}
		serviceLW.ScheduleRotation(context.Background())
		serviceLogWriter = serviceLW
	}

	ctx := context.Background()
	logger := logger.New(serviceLogWriter, slog.Level(*minLogLevel))

	// grpc
	cardSetGRPCConnection, err := grpc.Dial(*cardSetsGRPCAddress, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatalf("cardSetGRPCConnection did not connect: %v", err)
	}
	defer cardSetGRPCConnection.Close()
	cardSetGrpcClient := cardsetsgrpc.NewCardSetsClient(cardSetGRPCConnection)

	// app
	app, err := createApplication(
		ctx,
		logger,
		*redisAddress,
		*mongoURI,
		*enableCredentials,
		cardsets_client.NewClient(logger, cardSetGrpcClient),
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

	fmt.Printf("Starting cardSetPullWorker")
	err = app.startCardSetPullWorker()
	if err != nil {
		logger.ErrorWithError(context.Background(), err, "can't start cardSetPullWorker")
		return failCode
	}

	// Initialize a new http.Server struct.
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
