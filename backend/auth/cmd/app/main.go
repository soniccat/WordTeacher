package main

import (
	"flag"
	"fmt"
	"github.com/alexedwards/scs/redisstore"
	"github.com/alexedwards/scs/v2"
	"github.com/gomodule/redigo/redis"
	"log"
	"net/http"
	"os"
	"time"
)

func main() {
	// Define command-line flags
	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4000, "HTTP server network port")

	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017", "Database hostname url")
	//mongoDatabse := flag.String("mongoDatabase", "users", "Database name")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")

	flag.Parse()

	app, err := createApplication(redisAddress, mongoURI, enableCredentials)
	defer func() {
		app.stop()
	}()

	if err != nil {
		return
	}

	// Initialize a new http.Server struct.
	serverURI := fmt.Sprintf("%s:%d", *serverAddr, *serverPort)
	srv := &http.Server{
		Addr:         serverURI,
		ErrorLog:     app.logger.error,
		Handler:      app.routes(),
		IdleTimeout:  time.Minute,
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
	}

	app.logger.info.Printf("Starting server on %s", serverURI)
	err = srv.ListenAndServe()
	app.logger.error.Fatal(err)
}

func createApplication(
	redisAddress *string,
	mongoURI *string,
	enableCredentials *bool,
) (*application, error) {
	app := &application{
		logger:         createLogger(),
		sessionManager: createSessionManager(redisAddress),
	}
	err := app.setupMongo(mongoURI, enableCredentials)
	if err == nil {
		err = app.userModel.prepare(*app.mongoWrapper.context)
	}

	return app, err
}

func createSessionManager(redisAddress *string) *scs.SessionManager {
	pool := &redis.Pool{
		MaxIdle: 10,
		Dial: func() (redis.Conn, error) {
			return redis.Dial("tcp", *redisAddress)
		},
	}

	sessionManager := scs.New()
	sessionManager.Store = redisstore.New(pool)
	sessionManager.Lifetime = 24 * time.Hour
	return sessionManager
}

func createLogger() *logger {
	return &logger{
		error: log.New(os.Stderr, "ERROR\t", log.Ldate|log.Ltime|log.Lshortfile),
		info:  log.New(os.Stdout, "INFO\t", log.Ldate|log.Ltime),
	}
}
