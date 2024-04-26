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
const HeaderAppVersion = "X-App-Version"

const (
	DeviceTypeAndroid = "android"
	DeviceTypeDesktop = "desktop"
)

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

func SetError(w http.ResponseWriter, outErr error, code int, logger *logger.Logger) {
	if code >= 500 {
		logger.ErrorWithError(context.Background(), outErr, "")
	} else {
		logger.InfoWithError(context.Background(), outErr, "") // TODO: remove it, now it's here just for testing
	}

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
