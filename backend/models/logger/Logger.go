package logger

import (
	"log"
	"os"
)

type Logger struct {
	Error            *log.Logger
	Info             *log.Logger
	AllowStackTraces bool
}

func New(allowStackTraces bool) *Logger {
	return &Logger{
		Error:            log.New(os.Stderr, "ERROR\t", log.Ldate|log.Ltime|log.Lshortfile),
		Info:             log.New(os.Stdout, "INFO\t", log.Ldate|log.Ltime),
		AllowStackTraces: allowStackTraces,
	}
}
