package main

import (
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"github.com/alexedwards/scs/v2"
	"log"
	"models/session_validator"
	"net/http"
	"runtime/debug"
	cardSetsRabbitmq "service_cardsets/pkg/rabbitmq"
	"service_cardsetsearch/internal/cardsetsearch"
	"time"
	"tools"
	"tools/logger"
	"tools/mongowrapper"
	"tools/rabbitmq"
)

func main() {
	// Define command-line flags
	isDebug := flag.Bool("debugMode", false, "Shows stack traces in logs")
	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4002, "HTTP server network port")

	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	rabbitMQUrl := flag.String("rabbitMQ", "amqp://guest:guest@localhost:5672/", "RabbitMQ url")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")

	flag.Parse()

	ctx := context.Background()

	sessionManager := tools.CreateSessionManager(*redisAddress)
	app, err := createApplication(
		ctx,
		*isDebug,
		sessionManager,
		*mongoURI,
		*enableCredentials,
		*rabbitMQUrl,
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

	err = app.launchCardSetQueueConsuming(ctx)
	if err != nil {
		panic(err)
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

func (app *application) launchCardSetQueueConsuming(ctx context.Context) error {
	messages, err := app.cardSetQueue.Consume()
	if err != nil {
		return err
	}

	go func() {
		for m := range messages {
			log.Printf("Received a message from cardSetQueue: %s", m.Body)

			var parsedMessage cardSetsRabbitmq.Message
			err = json.Unmarshal(m.Body, &parsedMessage)
			if err != nil {
				app.logger.Error.Print("Unmarshal error for %s", m.Body)
				continue
			}

			err = app.handleMessage(ctx, parsedMessage)
			if err != nil {
				app.logger.Error.Print("error in error handling %s", err.Error())
				continue
			}

			err = m.Ack(false)
			if err != nil {
				app.logger.Error.Print("Ack error " + err.Error())
				continue
			}
		}
	}()

	return nil
}

func (app *application) handleMessage(ctx context.Context, parsedMessage cardSetsRabbitmq.Message) error {
	switch parsedMessage.Type {
	case cardSetsRabbitmq.TypeUpdate:
		cardSet, err := parsedMessage.GetCardSet()
		if err != nil {
			return err
		}

		err = app.cardSetSearchRepository.UpsertCardSet(ctx, cardSet) //app.handleUpdatedCardSet(ctx, cardSet)
		if err != nil {
			return err
		}
		// ignore for now
		//case cardSetsRabbitmq.TypeDelete:
		//	cardSetId, err := parsedMessage.GetDeletedCardSetId()
		//	if err != nil {
		//		return err
		//	}
		//
		//	mongoId, err := tools.ParseObjectID(*cardSetId)
		//	if err != nil {
		//		return err
		//	}
		//
		//	err = app.cardSetSearchRepository.DeleteCardSet(ctx, mongoId)
		//	if err != nil {
		//		return err
		//	}
	}

	return nil
}

func createApplication(
	ctx context.Context,
	isDebug bool,
	sessionManager *scs.SessionManager,
	mongoURI string,
	enableCredentials bool,
	rabbitMQUrl string,
	sessionValidator session_validator.SessionValidator,
) (_ *application, err error) {
	app := &application{
		logger:           logger.New(isDebug),
		sessionManager:   sessionManager,
		sessionValidator: sessionValidator,
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

	err = rabbitmq.SetupApp(app, rabbitMQUrl)
	if err != nil {
		return nil, err
	}

	app.cardSetQueue, err = app.rabbitMQ.QueueDeclare(rabbitmq.RabbitMQQueueCardSets)
	if err != nil {
		return nil, err
	}

	app.cardSetSearchRepository = cardsetsearch.New(app.logger, app.mongoWrapper.Client)
	err = app.cardSetSearchRepository.CreateTextIndexIfNeeded(ctx)
	if err != nil {
		return nil, err
	}

	return app, nil
}
