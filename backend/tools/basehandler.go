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

func (h *BaseHandler) SetError(w http.ResponseWriter, outErr error, code int) {
	SetError(w, outErr, code, h.Logger)
}

func (h *BaseHandler) WriteResponse(w http.ResponseWriter, response interface{}) {
	WriteResponse(w, response, h.Logger)
}
