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
	minLogLevel := flag.Int("logLevel", int(slog.LevelInfo), "minimum log level")
	serviceLogPath := flag.String("serviceLogPath", "/var/log", "service log file path")

	serverAddr := flag.String("serverAddr", "", "HTTP server network address")
	serverPort := flag.Int("serverPort", 4000, "HTTP server network port")
	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	redisAddress := flag.String("redisAddress", "localhost:6379", "redisAddress")
	enableCredentials := flag.Bool("enableCredentials", false, "Enable the use of credentials for mongo connection")
	flag.Parse()

	var googleConfigPath string
	var vkidConfigPath string
	var yandexIdConfigPath string
	var serviceLogWriter io.Writer
	var err error

	if *isDebug {
		googleConfigPath = "./../../../google"
		vkidConfigPath = "./../../../vkid"
		yandexIdConfigPath = "./../../../yandexid"

		serviceLogWriter = os.Stdout
	} else {
		googleConfigPath = "/run/secrets/google"
		vkidConfigPath = "/run/secrets/vkid"
		yandexIdConfigPath = "/run/secrets/yandexid"

		serviceLW, err := logger.NewLogWriter(*serviceLogPath, "service_", os.Stderr)
		if err != nil {
			fmt.Println("service NewLogWriter error: " + err.Error())
			return failCode
		}
		serviceLW.ScheduleRotation(context.Background())
		serviceLogWriter = serviceLW
	}

	// Parse configs
	var cfg service_models.Configs
	config.RequireJsonConfig(vkidConfigPath, &cfg.VKIDConfig)
	config.RequireJsonConfig(googleConfigPath, &cfg.GoogleConfig)
	config.RequireJsonConfig(yandexIdConfigPath, &cfg.YandexIdConfig)

	logger := logger.New(serviceLogWriter, slog.Level(*minLogLevel))
	timeProvider := time_provider.TimeProvider{}
	app, err := createApplication(logger, cfg, &timeProvider, *redisAddress, *mongoURI, *enableCredentials)

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

	// server
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
