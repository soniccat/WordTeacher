package apphelpers

import (
	"encoding/json"
	"fmt"
	"models/logger"
	"models/tools"
	"net/http"
	"runtime/debug"
)

const CookieSession = "session"
const HeaderDeviceId = "deviceId"

func NewHandlerError(code int, err error, withStack bool) *HandlerError {
	var stack *[]byte
	if withStack {
		stack = tools.Ptr(debug.Stack())
	}

	return &HandlerError{
		StatusCode: code,
		InnerError: err,
		Stack:      stack,
	}
}

func (v *HandlerError) Error() string {
	return v.InnerError.Error()
}

func SetHandlerError(w http.ResponseWriter, outErr *HandlerError, logger *logger.Logger) {
	var stack *[]byte
	if logger.AllowStackTraces {
		stack = outErr.Stack

		if stack == nil {
			stack = tools.Ptr(debug.Stack())
		}
	}

	SetErrorWithStack(w, outErr.InnerError, outErr.StatusCode, logger, stack)
}

type ErrorResponse struct {
	Error string `json:"error"`
}

type HandlerError struct {
	StatusCode int
	InnerError error
	Stack      *[]byte
}

func SetError(w http.ResponseWriter, outErr error, code int, logger *logger.Logger) {
	SetErrorWithStack(w, outErr, code, logger, tools.Ptr(debug.Stack()))
}

func SetErrorWithStack(w http.ResponseWriter, outErr error, code int, logger *logger.Logger, stack *[]byte) {
	w.WriteHeader(code)

	marshaledResponse, err := json.Marshal(ErrorResponse{Error: outErr.Error()})
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	if err = SetJsonData(w, marshaledResponse); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	if logger.AllowStackTraces {
		trace := fmt.Sprintf("%s\n%s", outErr.Error(), stack)
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
