package tools

import (
	"net/http"
	"tools/logger"
)

type BaseHandler struct {
	Logger *logger.Logger
}

func NewBaseHandler(logger *logger.Logger) *BaseHandler {
	return &BaseHandler{
		Logger: logger,
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
