package tools

import (
	"net/http"
	"tools/logger"
)

type BaseHandler struct {
	Logger *logger.Logger
}

func (app *BaseHandler) AllowStackTraces() bool {
	return app.Logger.AllowStackTraces
}

func (app *BaseHandler) NewHandlerError(code int, err error) *HandlerError {
	return NewHandlerError(err, code, app.AllowStackTraces())
}

func (app *BaseHandler) SetHandlerError(w http.ResponseWriter, err *HandlerError) {
	SetHandlerError(w, err, app.Logger)
}

func (app *BaseHandler) SetError(w http.ResponseWriter, outErr error, code int) {
	if handlerError, ok := outErr.(*HandlerError); ok {
		app.SetHandlerError(w, handlerError)
	} else {
		SetError(w, outErr, code, app.Logger)
	}
}

func (app *BaseHandler) WriteResponse(w http.ResponseWriter, response interface{}) {
	WriteResponse(w, response, app.Logger)
}
