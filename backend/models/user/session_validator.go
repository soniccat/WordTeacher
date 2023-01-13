package user

import (
	"encoding/json"
	"errors"
	"github.com/alexedwards/scs/v2"
	"models/apphelpers"
	"models/userauthtoken"
	"net/http"
)

type SessionValidator[T any] interface {
	Validate(r *http.Request) (*T, *userauthtoken.UserAuthToken, *ValidateSessionError)
}

type SessionManagerValidator[T any, PT TokenHolder[T]] struct {
	SessionManager *scs.SessionManager
}

func NewSessionManagerValidator[T any, PT TokenHolder[T]](sm *scs.SessionManager) SessionValidator[T] {
	return &SessionManagerValidator[T, PT]{sm}
}

//func (v *SessionManagerValidator[T, PT]) ToInterface() SessionValidator[T] {
//	return v
//}

func (v *SessionManagerValidator[T, PT]) Validate(r *http.Request) (*T, *userauthtoken.UserAuthToken, *ValidateSessionError) {
	return validateSession[T, PT](r, v.SessionManager)
}

func validateSession[T any, PT TokenHolder[T]](
	r *http.Request,
	sessionManager *scs.SessionManager,
) (*T, *userauthtoken.UserAuthToken, *ValidateSessionError) {
	_, err := r.Cookie(apphelpers.CookieSession)
	if err != nil {
		return nil, nil, NewValidateSessionError(http.StatusBadRequest, err)
	}

	// Header params
	var deviceId = r.Header.Get(apphelpers.HeaderDeviceId)
	if len(deviceId) == 0 {
		return nil, nil, NewValidateSessionError(http.StatusBadRequest, errors.New("invalid device id"))
	}

	var deviceType = r.Header.Get(apphelpers.HeaderDeviceType)
	if len(deviceType) == 0 {
		return nil, nil, NewValidateSessionError(http.StatusBadRequest, errors.New("invalid device type"))
	}

	// Parse session data and check if it's expired
	userAuthToken, err := userauthtoken.Load(r.Context(), sessionManager)
	if err != nil {
		return nil, nil, NewValidateSessionError(http.StatusUnauthorized, err)
	}

	if !userAuthToken.IsValid() {
		return nil, nil, NewValidateSessionError(http.StatusUnauthorized, errors.New("invalid auth token"))
	}

	// Body params
	var input T
	err = json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		return nil, nil, NewValidateSessionError(http.StatusBadRequest, err)
	}

	p := PT(&input)
	if !userAuthToken.IsMatched(
		p.GetAccessToken(),
		p.GetRefreshToken(),
		deviceType,
		deviceId,
	) {
		return nil, nil, NewValidateSessionError(http.StatusUnauthorized, errors.New("invalid auth token"))
	}

	return &input, userAuthToken, nil
}
