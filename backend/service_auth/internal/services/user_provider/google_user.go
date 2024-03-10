package user_provider

import (
	"context"
	"models"
	"tools"

	"google.golang.org/api/idtoken"

	"service_auth/internal/service_models"
)

func (s *Service) GoogleUser(
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
		idToken = s.googleConfig.GoogleIdTokenAndroidAudience
	} else {
		idToken = s.googleConfig.GoogleIdTokenDesktopAudience
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

	googleUser, err := s.userStorage.FindUserById(context, models.Google, googleUserId)
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
