package auth

import (
	"encoding/json"
	"errors"
	"models"
	"net/http"
	"service_auth/internal/service_models"
	"tools"
	"tools/logger"

	"github.com/google/uuid"
	"github.com/gorilla/mux"
)

type Handler struct {
	tools.BaseHandler
	authorizer authorizer
}

func New(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	authorizer authorizer,
) *Handler {
	return &Handler{
		BaseHandler: *tools.NewBaseHandler(logger, timeProvider),
		authorizer:  authorizer,
	}
}

type Input struct {
	Token string `json:"token,omitempty"`
}

type Response struct {
	Token ResponseToken `json:"token"`
	User  ResponseUser  `json:"user"`
}

type ResponseToken struct {
	AccessToken  string `json:"accessToken,omitempty"`
	RefreshToken string `json:"refreshToken,omitempty"`
}

type ResponseUser struct {
	Id          string `json:"id"`
	NetworkType string `json:"networkType"`
}

// Purpose:
//
//	Validate input credentials and if everything is fine, generate new access token and refresh token
//
// In:
//
//	Path: 	networkType
//	Header: deviceId
//	Body: 	AuthInput
//
// Out:
//
//	AuthResponse
func (h *Handler) Auth(w http.ResponseWriter, r *http.Request) {
	// Path params
	params := mux.Vars(r)
	networkType := params["networkType"]

	networkTypeInt, err := models.UserNetworkTypeFromString(networkType)
	if err != nil {
		h.SetError(w, err, http.StatusBadRequest)
		return
	}

	// Header params
	var deviceId = r.Header.Get(tools.HeaderDeviceId)
	if len(deviceId) == 0 {
		h.SetError(w, errors.New("deviceId is empty"), http.StatusBadRequest)
		return
	}

	var deviceType = r.Header.Get(tools.HeaderDeviceType)
	if len(deviceType) == 0 {
		h.SetError(w, errors.New("deviceType is empty"), http.StatusBadRequest)
		return
	}

	var appVersion = r.Header.Get(tools.HeaderAppVersion)
	if len(appVersion) == 0 {
		h.SetError(w, errors.New("appVersion is empty"), http.StatusBadRequest)
		return
	}

	// Body params
	var credentials Input
	err = json.NewDecoder(r.Body).Decode(&credentials)
	if err != nil {
		h.SetError(w, err, http.StatusBadRequest)
		return
	}

	ctx := logger.WrapContext(
		r.Context(),
		"logId", uuid.NewString(),
		"networkType", networkType,
		"deviceId", deviceId,
		"deviceType", deviceType,
		"appVersion", appVersion,
	)

	authorizedUser, err := h.authorizer.Authorize(
		ctx,
		credentials.Token,
		service_models.UserInfo{
			NetworkType: networkTypeInt,
			DeviceType:  deviceType,
			DeviceId:    deviceId,
			AppVersion:  appVersion,
		},
	)
	if _, ok := err.(*service_models.ErrorInvalidToken); ok {
		h.SetError(w, err, http.StatusUnauthorized)
		return

	} else if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}

	response := Response{
		Token: ResponseToken{
			AccessToken:  authorizedUser.Token.AccessToken.Value,
			RefreshToken: authorizedUser.Token.RefreshToken.Value,
		},
		User: ResponseUser{
			Id:          authorizedUser.UserId,
			NetworkType: networkType,
		},
	}

	h.WriteResponse(w, response)
}
