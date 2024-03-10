package main

import (
	"flag"
	"fmt"
	"net/http"
	"os"
	"runtime/debug"
	"service_auth/internal/service_models"
	"time"
	"tools/config"
	"tools/logger"
	"tools/time_provider"
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
	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4000, "HTTP server network port")

	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")
	flag.Parse()

	var googleConfigPath string
	var vkidConfigPath string
	if *isDebug {
		googleConfigPath = "./../../../google"
		vkidConfigPath = "./../../../vkid"
	} else {
		googleConfigPath = "/run/secrets/google"
		vkidConfigPath = "/run/secrets/vkid"
	}

	// Parse configs
	var cfg service_models.Configs
	config.RequireJsonConfig(vkidConfigPath, &cfg.VKIDConfig)
	config.RequireJsonConfig(googleConfigPath, &cfg.GoogleConfig)

	logger := logger.New(*isDebug)
	timeProvider := time_provider.TimeProvider{}
	app, err := createApplication(logger, cfg, &timeProvider, *redisAddress, *mongoURI, *enableCredentials)

	if err != nil {
		fmt.Println("app creation error: " + err.Error())
		return failCode
	}

	defer func() {
		app.stop()
	}()

	defer func() {
		if r := recover(); r != nil {
			fmt.Printf("stacktrace from panic: %v\n%s\n", r, string(debug.Stack()))
		}
		if err != nil {
			logger.Error.Print(err)
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

	logger.Info.Printf("Starting server on %s", serverURI)
	err = srv.ListenAndServe()
	logger.Error.Fatal(err)

	return successCode
}
