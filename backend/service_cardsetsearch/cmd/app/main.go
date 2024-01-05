package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"net/http"
	"runtime/debug"
	"time"
	"tools"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"

	"models/session_validator"
	cardsetsgrpc "service_cardsets/pkg/grpc/service_cardsets/api"
	"service_cardsetsearch/internal/cardsets_client"
	"tools/logger"
)

func main() {
	// flags
	isDebug := flag.Bool("debugMode", false, "Shows stack traces in logs")
	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4002, "HTTP server network port")

	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	cardSetsGRPCAddress := flag.String("cardSetsGPRCAddress", "localhost:5001", "get new cardsets")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")

	flag.Parse()

	ctx := context.Background()
	logger := logger.New(*isDebug)
	sessionManager := tools.CreateSessionManager(*redisAddress)

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
		sessionManager,
		*mongoURI,
		*enableCredentials,
		session_validator.NewSessionManagerValidator(sessionManager),
		cardsets_client.NewClient(logger, cardSetGrpcClient),
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

	logger.Info.Printf("Starting cardSetPullWorker")
	err = app.startCardSetPullWorker()
	if err != nil {
		logger.Error.Fatal(err)
		return
	}

	// Initialize a new http.Server struct.
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
