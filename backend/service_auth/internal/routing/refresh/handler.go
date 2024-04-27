package refresh

import (
	"encoding/json"
	"errors"
	"net/http"
	"service_auth/internal/service_models"
	"tools"
	"tools/logger"

	"github.com/google/uuid"
)

type Handler struct {
	tools.BaseHandler
	tokenRefresher tokenRefresher
}

type Input struct {
	AccessToken  string `json:"accessToken,omitempty"`
	RefreshToken string `json:"refreshToken,omitempty"`
}

type Response struct {
	AccessToken  string `json:"accessToken,omitempty"`
	RefreshToken string `json:"refreshToken,omitempty"`
}

func New(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	tokenRefresher tokenRefresher,
) *Handler {
	return &Handler{
		BaseHandler:    *tools.NewBaseHandler(logger, timeProvider),
		tokenRefresher: tokenRefresher,
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
func (h *Handler) Refresh(w http.ResponseWriter, r *http.Request) {
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

	var appVersion = r.Header.Get(tools.HeaderAppVersion)
	if len(appVersion) == 0 {
		h.SetError(w, errors.New("appVersion is empty"), http.StatusBadRequest)
		return
	}

	// Body params
	var input Input
	err = json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		h.SetError(w, err, http.StatusBadRequest)
		return
	}

	ctx := logger.WrapContext(
		r.Context(),
		"logId", uuid.NewString(),
		"deviceId", deviceId,
		"deviceType", deviceType,
		"appVersion", appVersion,
	)

	authorizedUser, err := h.tokenRefresher.RefreshToken(
		ctx,
		service_models.UserTokens{
			AccessToken:  input.AccessToken,
			RefreshToken: &input.RefreshToken,
		},
		service_models.UserInfo{
			DeviceType: deviceType,
			DeviceId:   deviceId,
		},
	)
	if _, ok := err.(*service_models.ErrorInvalidToken); ok {
		h.SetError(w, err, http.StatusUnauthorized)
		return

	} else if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}

	// Build response
	response := Response{
		AccessToken:  authorizedUser.Token.AccessToken.Value,
		RefreshToken: authorizedUser.Token.RefreshToken,
	}

	h.WriteResponse(w, response)
}
