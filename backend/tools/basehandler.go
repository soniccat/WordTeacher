package tools

import (
	"net/http"
	"time"
	"tools/logger"
)

type BaseHandler struct {
	Logger       *logger.Logger
	TimeProvider TimeProvider
}

type TimeProvider interface {
	Now() time.Time
}

func NewBaseHandler(logger *logger.Logger, TimeProvider TimeProvider) *BaseHandler {
	return &BaseHandler{
		Logger:       logger,
		TimeProvider: TimeProvider,
	}
}

func (h *BaseHandler) AllowStackTraces() bool {
	return h.Logger.AllowStackTraces
}

func (h *BaseHandler) NewHandlerError(code int, err error) *HandlerError {
	return NewHandlerError(err, code, h.AllowStackTraces())
}

func (h *BaseHandler) SetHandlerError(w http.ResponseWriter, err *HandlerError) {
	SetHandlerError(w, err, h.Logger)
}

func (h *BaseHandler) SetError(w http.ResponseWriter, outErr error, code int) {
	if handlerError, ok := outErr.(*HandlerError); ok {
		h.SetHandlerError(w, handlerError)
	} else {
		SetError(w, outErr, code, h.Logger)
	}
}

func (h *BaseHandler) WriteResponse(w http.ResponseWriter, response interface{}) {
	WriteResponse(w, response, h.Logger)
}
