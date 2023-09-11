package routing

import (
	"api"
	"context"
	"encoding/json"
	"errors"
	"models"
	"net/http"
	"tools"
	"tools/logger"

	"github.com/gorilla/mux"
	"google.golang.org/api/idtoken"

	"service_auth/internal/storage"
	userauthtokengenerator "service_auth/internal/userauthtoken_generator"
)

// TODO: move in params
const GoogleIdTokenAndroidAudience = "435809636010-8kf32mn6jdokebe03cd9g8p2giudiq1c.apps.googleusercontent.com"
const GoogleIdDesktopTokenAudience = "166526384655-9ji25ddl02vg3d91g8vc2tbvbupl6o3k.apps.googleusercontent.com"

type AuthInput struct {
	Token string `json:"token,omitempty"`
}

type AuthResponse struct {
	Token AuthResponseToken `json:"token"`
	User  AuthResponseUser  `json:"user"`
}

type CardSetGetByIdResponse struct {
	CardSet *api.CardSet `json:"cardSet"`
}

type AuthHandler struct {
	tools.BaseHandler
	userRepository         *storage.UserRepository
	userAuthTokenGenerator userauthtokengenerator.UserAuthTokenGenerator
}

func NewAuthHandler(
	logger *logger.Logger,
	userRepository *storage.UserRepository,
	userAuthTokenGenerator userauthtokengenerator.UserAuthTokenGenerator,
) *AuthHandler {
	return &AuthHandler{
		BaseHandler:            *tools.NewBaseHandler(logger),
		userRepository:         userRepository,
		userAuthTokenGenerator: userAuthTokenGenerator,
	}
}

type AuthResponseToken struct {
	AccessToken  string `json:"accessToken,omitempty"`
	RefreshToken string `json:"refreshToken,omitempty"`
}

type AuthResponseUser struct {
	Id          string `json:"id"`
	NetworkType string `json:"networkType"`
}

type AuthErrorInvalidToken struct {
	s string
}

func NewAuthErrorInvalidToken(str string) *AuthErrorInvalidToken {
	return &AuthErrorInvalidToken{
		s: str,
	}
}

func (e *AuthErrorInvalidToken) Error() string { return e.s }

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
func (h *AuthHandler) Auth(w http.ResponseWriter, r *http.Request) {
	// Path params
	params := mux.Vars(r)
	networkType := params["networkType"]

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
	var credentials AuthInput
	err := json.NewDecoder(r.Body).Decode(&credentials)
	if err != nil {
		h.SetError(w, err, http.StatusBadRequest)
		return
	}

	// Resolve user and userNetwork
	var aUser *models.User
	var userNetwork *models.UserNetwork

	if networkType == "google" {
		aUser, userNetwork, err = h.resolveGoogleUser(r.Context(), &credentials, deviceType)

		if _, ok := err.(*AuthErrorInvalidToken); ok {
			h.SetError(w, err, http.StatusUnauthorized)
			return

		} else if err != nil {
			h.SetError(w, err, http.StatusInternalServerError)
			return
		}

	} else {
		h.SetError(w, errors.New("unknown networkType"), http.StatusBadRequest)
		return
	}

	// Create user if needed
	if aUser == nil {
		aUser = &models.User{
			Networks: []models.UserNetwork{*userNetwork},
		}
		newUser, err := h.userRepository.InsertUser(r.Context(), aUser)
		if err != nil {
			h.SetError(w, err, http.StatusInternalServerError)
			return
		}

		aUser = newUser
	} else {
		// TODO: add new network if needed
	}

	// Create new access token / refresh token pair
	token, err := h.userAuthTokenGenerator.Generate(
		r.Context(),
		&aUser.Id,
		userNetwork.NetworkType,
		deviceType,
		deviceId,
	)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}

	// Build response
	response := AuthResponse{
		Token: AuthResponseToken{
			AccessToken:  token.AccessToken.Value,
			RefreshToken: token.RefreshToken,
		},
		User: AuthResponseUser{
			Id:          aUser.Id.Hex(),
			NetworkType: networkType,
		},
	}

	h.WriteResponse(w, response)
}

func (h *AuthHandler) resolveGoogleUser(
	context context.Context,
	credentials *AuthInput,
	deviceType string,
) (*models.User, *models.UserNetwork, error) {

	validator, err := idtoken.NewValidator(context)
	if err != nil {
		return nil, nil, err
	}

	var idToken string
	if deviceType == tools.DeviceTypeAndroid {
		idToken = GoogleIdTokenAndroidAudience
	} else {
		idToken = GoogleIdDesktopTokenAudience
	}

	// TODO: consider to make validation more strict
	payload, err := validator.Validate(context, credentials.Token, idToken)
	if err != nil {
		return nil, nil, NewAuthErrorInvalidToken(err.Error())
	}

	googleUserId, ok := payload.Claims["sub"].(string)
	if !ok {
		return nil, nil, NewAuthErrorInvalidToken("google Id Token doesn't have a sub")
	}

	googleEmail, ok := payload.Claims["email"].(string)
	if !ok {
		return nil, nil, NewAuthErrorInvalidToken("google Id Token doesn't have an email")
	}

	googleName, ok := payload.Claims["name"].(string)
	if !ok {
		return nil, nil, NewAuthErrorInvalidToken("google Id Token doesn't have a name")
	}

	googleUser, err := h.userRepository.FindGoogleUser(context, &googleUserId)
	if err != nil {
		return nil, nil, err
	}

	// TODO: if user exists, update its network if it changed
	userNetwork := &models.UserNetwork{
		NetworkType:   models.Google,
		NetworkUserId: googleUserId,
		Email:         googleEmail,
		Name:          googleName,
	}

	return googleUser, userNetwork, nil
}
