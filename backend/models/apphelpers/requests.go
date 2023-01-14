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
const HeaderDeviceType = "deviceType"

type ErrorWithCode struct {
	Err  error
	Code int
}

func NewErrorWithCode(err error, code int) *ErrorWithCode {
	return &ErrorWithCode{err, code}
}

func NewHandlerError(err error, code int, withStack bool) *HandlerError {
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

const (
	ResponseStatusOk    = "ok"
	ResponseStatusError = "error"
)

type responseErrorWrapper struct {
	Message string `json:"message"`
}

func newResponseErrorWrapper(err error) *responseErrorWrapper {
	return &responseErrorWrapper{
		err.Error(),
	}
}

type Response struct {
	Status string      `json:"status"`
	Value  interface{} `json:"value"`
}

func NewResponseOk(data interface{}) *Response {
	return &Response{
		ResponseStatusOk,
		data,
	}
}

func NewResponseError(err error) *Response {
	return &Response{
		ResponseStatusError,
		newResponseErrorWrapper(err),
	}
}

type ErrorResponse struct {
	Message string `json:"message"`
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

	marshaledResponse, err := json.Marshal(NewResponseError(outErr))
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	if err = setJsonData(w, marshaledResponse); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	if logger.AllowStackTraces {
		trace := fmt.Sprintf("%s\n%s", outErr.Error(), stack)
		err = logger.Error.Output(2, trace)
	}
}

func setJsonData(w http.ResponseWriter, data []byte) error {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")

	if _, err := w.Write(data); err != nil {
		return err
	}

	return nil
}

func WriteResponse(w http.ResponseWriter, response interface{}, logger *logger.Logger) {
	marshaledResponse, err := json.Marshal(NewResponseOk(response))
	if err != nil {
		SetError(w, err, http.StatusInternalServerError, logger)
		return
	}

	if _, err = w.Write(marshaledResponse); err != nil {
		SetError(w, err, http.StatusInternalServerError, logger)
		return
	}
}
