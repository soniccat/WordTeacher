package tools

import (
	"context"
	"encoding/json"
	"net/http"
	"tools/logger"
)

const CookieSession = "session"
const HeaderDeviceId = "X-Device-Id"
const HeaderDeviceType = "X-Device-Type"
const HeaderAccessToken = "X-Access-Token"

const (
	DeviceTypeAndroid = "android"
	DeviceTypeDesktop = "desktop"
)

// const (
// 	ErrorWrongInput = 1000
// 	ErrorInnerError = 1001
// )

// type ErrorWithCode struct {
// 	Err  error
// 	Code int
// }

// func (e ErrorWithCode) Error() string {
// 	return e.Err.Error()
// }

// func NewErrorWithCode(err error, code int) *ErrorWithCode {
// 	return &ErrorWithCode{err, code}
// }

// func NewHandlerError(err error, code int, withStack bool) *HandlerError {
// 	var stack *[]byte
// 	if withStack {
// 		stack = Ptr(debug.Stack())
// 	}

// 	return &HandlerError{
// 		StatusCode: code,
// 		InnerError: err,
// 		Stack:      stack,
// 	}
// }

// func (v *HandlerError) Error() string {
// 	return v.InnerError.Error()
// }

// func SetHandlerError(w http.ResponseWriter, outErr *HandlerError, logger *logger.Logger) {
// 	var stack *[]byte
// 	if logger.AllowStackTraces {
// 		stack = outErr.Stack

// 		if stack == nil {
// 			stack = Ptr(debug.Stack())
// 		}
// 	}

// 	SetErrorWithStack(w, outErr.InnerError, outErr.StatusCode, logger, stack)
// }

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

// type HandlerError struct {
// 	StatusCode int
// 	InnerError error
// 	Stack      *[]byte
// }

func SetError(w http.ResponseWriter, outErr error, code int, logger *logger.Logger) {
	if code >= 500 {
		logger.ErrorWithError(context.Background(), outErr, "")
	} /*else {
		logger.InfoWithError(context.Background(), outErr, "") // TODO: remove it, now it's here just for testing
	}*/

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

	// if logger.AllowStackTraces {
	// 	trace := fmt.Sprintf("%s\n%s", outErr.Error(), stack)
	// 	err = logger.Error.Output(2, trace)
	// }
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
