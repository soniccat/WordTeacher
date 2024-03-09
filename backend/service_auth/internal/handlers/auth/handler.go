package auth

import (
	"encoding/json"
	"errors"
	"models"
	"net/http"
	serviceModels "service_auth/internal/models"
	"service_auth/internal/services/user_provider"
	"tools"
	"tools/logger"

	"github.com/gorilla/mux"
)

type Input struct {
	Token string `json:"token,omitempty"`
}

type Response struct {
	Token ResponseToken `json:"token"`
	User  ResponseUser  `json:"user"`
}

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

	// Body params
	var credentials Input
	err = json.NewDecoder(r.Body).Decode(&credentials)
	if err != nil {
		h.SetError(w, err, http.StatusBadRequest)
		return
	}

	authorizedUser, err := h.authorizer.Authorize(
		r.Context(),
		serviceModels.UserInfo{
			NetworkType: networkTypeInt,
			Token:       credentials.Token,
			DeviceType:  deviceType,
			DeviceId:    deviceId,
		},
	)
	if _, ok := err.(*user_provider.ErrorInvalidToken); ok {
		h.SetError(w, err, http.StatusUnauthorized)
		return

	} else if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}

	response := Response{
		Token: ResponseToken{
			AccessToken:  authorizedUser.Token.AccessToken.Value,
			RefreshToken: authorizedUser.Token.RefreshToken,
		},
		User: ResponseUser{
			Id:          authorizedUser.Id.Hex(),
			NetworkType: networkType,
		},
	}

	h.WriteResponse(w, response)
}
