package apphelpers

import (
	"encoding/json"
	"fmt"
	"models/logger"
	"net/http"
	"runtime/debug"
)

const CookieSession = "session"
const HeaderDeviceId = "deviceId"

type ErrorResponse struct {
	Error string
	Code  int
}

func SetError(w http.ResponseWriter, err error, code int, logger *logger.Logger) {
	w.WriteHeader(code)

	marshaledResponse, err := json.Marshal(ErrorResponse{Error: err.Error()})
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	if err = SetJsonData(w, marshaledResponse); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	if logger.AllowStackTraces {
		trace := fmt.Sprintf("%s\n%s", err.Error(), debug.Stack())
		err = logger.Error.Output(2, trace)
	}
}

func SetJsonData(w http.ResponseWriter, data []byte) error {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")

	if _, err := w.Write(data); err != nil {
		return err
	}

	return nil
}
