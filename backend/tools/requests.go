package tools

import (
	"compress/gzip"
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

	if err := setJsonData(w, NewResponseError(outErr)); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}

func WriteResponse(w http.ResponseWriter, response interface{}, logger *logger.Logger) {
	if err := setJsonData(w, NewResponseOk(response)); err != nil {
		SetError(w, err, http.StatusInternalServerError, logger)
		return
	}
}

func setJsonData(w http.ResponseWriter, obj any) error {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("Content-Encoding", "gzip")

	gw := gzip.NewWriter(w)
	je := json.NewEncoder(gw)

	defer gw.Close()

	if err := je.Encode(obj); err != nil {
		return err
	}

	return nil
}
