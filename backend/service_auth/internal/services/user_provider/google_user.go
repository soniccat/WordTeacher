package user_provider

import (
	"context"
	"models"
	"tools"

	"google.golang.org/api/idtoken"

	"service_auth/internal/service_models"
)

// TODO: move in params
const GoogleIdTokenAndroidAudience = "435809636010-8kf32mn6jdokebe03cd9g8p2giudiq1c.apps.googleusercontent.com"
const GoogleIdDesktopTokenAudience = "166526384655-9ji25ddl02vg3d91g8vc2tbvbupl6o3k.apps.googleusercontent.com"

func (h *Service) GoogleUser(
	context context.Context,
	token string,
	deviceType string,
) (*service_models.UserWithNetwork, error) {

	validator, err := idtoken.NewValidator(context)
	if err != nil {
		return nil, err
	}

	var idToken string
	if deviceType == tools.DeviceTypeAndroid {
		idToken = GoogleIdTokenAndroidAudience
	} else {
		idToken = GoogleIdDesktopTokenAudience
	}

	// TODO: consider to make validation more strict
	payload, err := validator.Validate(context, token, idToken)
	if err != nil {
		return nil, service_models.NewErrorInvalidToken(err.Error())
	}

	googleUserId, ok := payload.Claims["sub"].(string)
	if !ok {
		return nil, service_models.NewErrorInvalidToken("google Id Token doesn't have a sub")
	}

	googleUser, err := h.userStorage.FindUserById(context, models.Google, googleUserId)
	if err != nil {
		return nil, err
	}

	return &service_models.UserWithNetwork{
		User: googleUser,
		Network: models.UserNetwork{
			NetworkType:   models.Google,
			NetworkUserId: googleUserId,
		},
	}, nil
}
