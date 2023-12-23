package main

import (
	"flag"
	"fmt"
	"net"
	"net/http"
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

func main() {
	// Define command-line flags
	isDebug := flag.Bool("debugMode", false, "Shows stack traces in logs")
	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4001, "HTTP server network port")

	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	grpcPort := flag.Int("grpcPort", 5001, "gRPC port")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")

	flag.Parse()

	logger := logger.New(*isDebug)
	storage, err := storage.New(
		logger,
		*mongoURI,
		*enableCredentials,
	)
	if err != nil {
		fmt.Println("app creation error: " + err.Error())
		return
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

	// grpc
	grpcServer := grpc.NewServer()
	rpcCardSetService := NewCardSetServer(app)
	cardsetsgrpc.RegisterCardSetsServer(grpcServer, rpcCardSetService)

	grpcListener, err := net.Listen("tcp", fmt.Sprintf(":%d", *grpcPort))
	if err != nil {
		logger.Error.Printf("grpc: failed to listen: %v", err)
	}

	go func() {
		if err := grpcServer.Serve(grpcListener); err != nil {
			logger.Error.Printf("grpc: failed to serve: %v", err)
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
