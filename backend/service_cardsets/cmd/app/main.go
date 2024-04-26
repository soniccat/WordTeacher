package main

import (
	"context"
	"flag"
	"fmt"
	"io"
	"log"
	"log/slog"
	"net"
	"net/http"
	"os"
	"runtime/debug"
	"time"
	"tools"
	"tools/logger"
	"tools/time_provider"

	"google.golang.org/grpc"

	"models/session_validator"
	"service_cardsets/internal/storage"
	cardsetsgrpc "service_cardsets/pkg/grpc/service_cardsets/api"
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
	serverLogPath := flag.String("serverLogPath", "/var/log", "server log file path")

	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4001, "HTTP server network port")
	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	grpcPort := flag.Int("grpcPort", 5001, "gRPC port")
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

	logger := logger.New(serviceLogWriter, slog.Level(*minLogLevel))
	storage, err := storage.New(
		logger,
		*mongoURI,
		*enableCredentials,
	)
	if err != nil {
		logger.ErrorWithError(context.Background(), err, "storage creation error")
		return failCode
	}

	sessionManager := tools.CreateSessionManager(*redisAddress)
	app, err := createApplication(
		logger,
		&time_provider.TimeProvider{},
		sessionManager,
		session_validator.NewSessionManagerValidator(sessionManager),
		storage,
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

	// grpc
	grpcServer := grpc.NewServer()
	rpcCardSetService := NewCardSetServer(app)
	cardsetsgrpc.RegisterCardSetsServer(grpcServer, rpcCardSetService)

	grpcListener, err := net.Listen("tcp", fmt.Sprintf(":%d", *grpcPort))
	if err != nil {
		logger.ErrorWithError(context.Background(), err, "grpc: failed to listen")
	}

	go func() {
		if err := grpcServer.Serve(grpcListener); err != nil {
			logger.ErrorWithError(context.Background(), err, "grpc: failed to serve")
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
