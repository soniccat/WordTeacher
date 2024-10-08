package token_refresher

import (
	"context"
	"models"
	"tools/logger"

	"service_auth/internal/service_models"
)

func (s *Service) RefreshToken(
	ctx context.Context,
	userTokens service_models.UserTokens,
	userInfo service_models.UserInfo,
) (*service_models.AuthorizedUser, error) {
	userAuthToken, err := models.Load(ctx, s.sessionManager)
	if err != nil {
		return nil, err
	}

	if !userAuthToken.IsRefreshTokenValid() {
		return nil, service_models.NewErrorInvalidToken(logger.Error(ctx, "token is invalid"))
	}

	err = userAuthToken.Match(
		ctx,
		userTokens.AccessToken,
		userTokens.RefreshToken,
		userInfo.DeviceType,
		userInfo.DeviceId,
	)

	if err != nil {
		return nil, service_models.NewErrorInvalidToken(err)
	}

	token, err := s.authTokenGenerator.Generate(
		ctx,
		userAuthToken.UserDbId,
		userAuthToken.NetworkType,
		userInfo.DeviceType,
		userInfo.DeviceId,
		userInfo.AppVersion,
	)
	if err != nil {
		return nil, err
	}

	return &service_models.AuthorizedUser{
		UserId:      userAuthToken.UserDbId,
		Token:       token,
		NetworkType: userInfo.NetworkType,
		DeviceType:  userInfo.DeviceType,
		DeviceId:    userInfo.DeviceId,
	}, nil
}
