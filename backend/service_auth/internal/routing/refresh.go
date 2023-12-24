package routing

import (
	"encoding/json"
	"errors"
	"models"
	"net/http"
	"tools"
	"tools/logger"

	"github.com/alexedwards/scs/v2"

	"service_auth/internal/storage"
	userauthtokengenerator "service_auth/internal/userauthtoken_generator"
)

type RefreshInput struct {
	AccessToken  string `json:"accessToken,omitempty"`
	RefreshToken string `json:"refreshToken,omitempty"`
}

type RefreshResponse struct {
	AccessToken  string `json:"accessToken,omitempty"`
	RefreshToken string `json:"refreshToken,omitempty"`
}

type RefreshHandler struct {
	tools.BaseHandler
	sessionManager         *scs.SessionManager
	userRepository         *storage.UserRepository
	userAuthTokenGenerator userauthtokengenerator.UserAuthTokenGenerator
}

func NewRefreshHandler(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionManager *scs.SessionManager,
	userRepository *storage.UserRepository,
	userAuthTokenGenerator userauthtokengenerator.UserAuthTokenGenerator,
) *RefreshHandler {
	return &RefreshHandler{
		BaseHandler:            *tools.NewBaseHandler(logger, timeProvider),
		sessionManager:         sessionManager,
		userRepository:         userRepository,
		userAuthTokenGenerator: userAuthTokenGenerator,
	}
}

// Purpose:
//
// In:
//
//	Header: deviceId
//	Body: RefreshInput
//
// Out:
//
//	RefreshResponse
func (h *RefreshHandler) Refresh(w http.ResponseWriter, r *http.Request) {
	session, err := r.Cookie(tools.CookieSession)
	if err != nil {
		h.SetError(w, err, http.StatusUnauthorized)
		return
	}
	if len(session.Value) == 0 {
		h.SetError(w, errors.New("session is empty"), http.StatusBadRequest)
		return
	}

	// Header params
	var deviceId = r.Header.Get(tools.HeaderDeviceId)
	if len(deviceId) == 0 {
		h.SetError(w, errors.New("DeviceId is empty"), http.StatusBadRequest)
		return
	}

	var deviceType = r.Header.Get(tools.HeaderDeviceType)
	if len(deviceType) == 0 {
		h.SetError(w, errors.New("DeviceType is empty"), http.StatusBadRequest)
		return
	}

	// Body params
	var input RefreshInput
	err = json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		h.SetError(w, err, http.StatusBadRequest)
		return
	}

	userAuthToken, err := models.Load(r.Context(), h.sessionManager)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}

	if !userAuthToken.IsValid() {
		h.SetError(w, errors.New("token is invalid"), http.StatusUnauthorized)
		return
	}

	if !userAuthToken.IsMatched(
		input.AccessToken,
		&input.RefreshToken,
		deviceType,
		deviceId,
	) {
		h.SetError(w, errors.New("token is invalid"), http.StatusUnauthorized)
		return
	}

	token, err := h.userAuthTokenGenerator.Generate(
		r.Context(),
		userAuthToken.UserDbId,
		userAuthToken.NetworkType,
		deviceType,
		deviceId,
	)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}

	// Build response
	response := RefreshResponse{
		AccessToken:  token.AccessToken.Value,
		RefreshToken: token.RefreshToken,
	}

	h.WriteResponse(w, response)
}
