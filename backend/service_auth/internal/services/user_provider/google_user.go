package user_provider

import (
	"context"
	"models"
	"tools"
	"tools/logger"

	"google.golang.org/api/idtoken"

	"service_auth/internal/service_models"
)

func (s *Service) GoogleUser(
	ctx context.Context,
	token string,
	deviceType string,
) (*service_models.UserWithNetwork, error) {

	validator, err := idtoken.NewValidator(ctx)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	var idToken string
	if deviceType == tools.DeviceTypeAndroid {
		idToken = s.googleConfig.GoogleIdTokenAndroidAudience
	} else {
		idToken = s.googleConfig.GoogleIdTokenDesktopAudience
	}

	// TODO: consider to make validation more strict
	payload, err := validator.Validate(ctx, token, idToken)
	if err != nil {
		return nil, service_models.NewErrorInvalidToken(logger.WrapError(ctx, err))
	}

	googleUserId, ok := payload.Claims["sub"].(string)
	if !ok {
		return nil, service_models.NewErrorInvalidToken(logger.Error(ctx, "google Id Token doesn't have a sub"))
	}

	googleUser, err := s.userStorage.FindUserById(ctx, models.Google, googleUserId)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	return &service_models.UserWithNetwork{
		User: googleUser,
		Network: models.UserNetwork{
			NetworkType:   models.Google,
			NetworkUserId: googleUserId,
		},
	}, nil
}
