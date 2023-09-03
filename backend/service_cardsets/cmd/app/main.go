package main

import (
	"context"
	"flag"
	"fmt"
	"models/session_validator"
	"net"
	"net/http"
	"runtime/debug"
	"service_cardsets/internal/cardset"
	"time"
	"tools"
	"tools/logger"
	"tools/mongowrapper"

	cardsets "service_cardsets/grpc"

	"google.golang.org/grpc"

	"github.com/alexedwards/scs/v2"
)

type CardSetServer struct {
	cardsets.UnimplementedCardSetsServer

	logger *logger.Logger
}

func NewCardSetServer(logger *logger.Logger) *CardSetServer {
	return &CardSetServer{
		logger: logger,
	}
}

func (s *CardSetServer) GetCardSets(in *cardsets.GetCardSetsIn, server cardsets.CardSets_GetCardSetsServer) error {
	s.logger.Info.Print("GetCardSets called")
	return nil
}

func (s *CardSetServer) GetCardSetById(ctx context.Context, in *cardsets.GetCardSetIn) (*cardsets.GetCardSetOut, error) {
	return nil, nil
}

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

	sessionManager := tools.CreateSessionManager(*redisAddress)
	app, err := createApplication(
		*isDebug,
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

	// rpc
	grpcServer := grpc.NewServer()
	rpcCardSetService := NewCardSetServer(app.logger)
	cardsets.RegisterCardSetsServer(grpcServer, rpcCardSetService)

	grpcListener, err := net.Listen("tcp", fmt.Sprintf(":%d", *grpcPort))
	if err != nil {
		app.logger.Error.Printf("grpc: failed to listen: %v", err)
	}

	go func() {
		if err := grpcServer.Serve(grpcListener); err != nil {
			app.logger.Error.Printf("grpc: failed to serve: %v", err)
		}
	}()

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
	sessionManager *scs.SessionManager,
	mongoURI string,
	enableCredentials bool,
	sessionValidator session_validator.SessionValidator,
) (_ *application, err error) {
	app := &application{
		logger:                logger.New(isDebug),
		sessionManager:        sessionManager,
		sessionValidator:      sessionValidator,
		cardSetMessageChannel: make(chan []byte),
	}

	defer func() {
		if err != nil {
			app.stop()
		}
	}()

	err = mongowrapper.SetupMongo(app, mongoURI, enableCredentials)
	if err != nil {
		return nil, err
	}

	app.cardSetRepository = cardset.New(app.logger, app.mongoWrapper.Client)

	return app, nil
}
