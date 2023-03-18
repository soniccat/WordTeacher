package models

import (
	"errors"
	"github.com/alexedwards/scs/v2"
	"net/http"
	"tools/apphelpers"
)

type ValidateSessionError struct {
	StatusCode int
	InnerError error
}

func NewValidateSessionError(code int, err error) *ValidateSessionError {
	return &ValidateSessionError{
		StatusCode: code,
		InnerError: err,
	}
}

func (v *ValidateSessionError) Error() string {
	return v.InnerError.Error()
}

type SessionValidator interface {
	Validate(r *http.Request) (*UserAuthToken, *ValidateSessionError)
}

type SessionManagerValidator struct {
	SessionManager *scs.SessionManager
}

func NewSessionManagerValidator(sm *scs.SessionManager) SessionValidator {
	return &SessionManagerValidator{sm}
}

//func (v *SessionManagerValidator[T, PT]) ToInterface() SessionValidator[T] {
//	return v
//}

func (v *SessionManagerValidator) Validate(r *http.Request) (*UserAuthToken, *ValidateSessionError) {
	return validateSession(r, v.SessionManager)
}

func validateSession(
	r *http.Request,
	sessionManager *scs.SessionManager,
) (*UserAuthToken, *ValidateSessionError) {
	_, err := r.Cookie(apphelpers.CookieSession)
	if err != nil {
		return nil, NewValidateSessionError(http.StatusUnauthorized, err)
	}

	// Header params
	var deviceId = r.Header.Get(apphelpers.HeaderDeviceId)
	if len(deviceId) == 0 {
		return nil, NewValidateSessionError(http.StatusBadRequest, errors.New("invalid device id"))
	}

	var deviceType = r.Header.Get(apphelpers.HeaderDeviceType)
	if len(deviceType) == 0 {
		return nil, NewValidateSessionError(http.StatusBadRequest, errors.New("invalid device type"))
	}

	// Parse session data and check if it's expired
	userAuthToken, err := Load(r.Context(), sessionManager)
	if err != nil {
		return nil, NewValidateSessionError(http.StatusUnauthorized, err)
	}

	if !userAuthToken.IsValid() {
		return nil, NewValidateSessionError(http.StatusUnauthorized, errors.New("invalid auth token"))
	}

	if !userAuthToken.IsMatched(
		r.Header.Get(apphelpers.HeaderAccessToken),
		nil,
		deviceType,
		deviceId,
	) {
		return nil, NewValidateSessionError(http.StatusUnauthorized, errors.New("invalid auth token"))
	}

	return userAuthToken, nil
}
