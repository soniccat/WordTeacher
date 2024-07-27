package userauthtoken_generator

import (
	"context"
	"models"
	"time"
	"tools/logger"

	"github.com/alexedwards/scs/v2"
	"github.com/google/uuid"
)

func (g *Service) Generate(
	ctx context.Context,
	userDbId string,
	networkType models.UserNetworkType,
	deviceType string,
	deviceId string,
	appVersion string,
) (*models.UserAuthToken, error) {
	token, err := generateUserAuthToken(
		ctx,
		userDbId,
		networkType,
		deviceType,
		deviceId,
		appVersion,
	)
	if err != nil {
		return nil, err
	}

	token, err = g.userRepository.InsertUserAuthToken(ctx, token)
	if err != nil {
		return nil, err
	}

	saveUserAuthTokenAsSession(token, ctx, g.sessionManager)

	return token, nil
}

func generateUserAuthToken(
	ctx context.Context,
	userDbId string,
	networkType models.UserNetworkType,
	deviceType string,
	deviceId string,
	appVersion string,
) (*models.UserAuthToken, error) {
	accessTokenValue, err := uuid.NewRandom()
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	refreshTokenValue, err := uuid.NewRandom()
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	return &models.UserAuthToken{
		UserDbId:    userDbId,
		NetworkType: networkType,
		AccessToken: models.AccessToken{
			Value:          accessTokenValue.String(),
			ExpirationDate: time.Now().Add(models.AccessTokenTimeout),
		},
		RefreshToken: models.RefreshToken{
			Value:          refreshTokenValue.String(),
			ExpirationDate: time.Now().Add(models.RefreshTokenTimeout),
		},
		UserDeviceType: deviceType,
		UserDeviceId:   deviceId,
		AppVersion:     appVersion,
	}, nil
}

func saveUserAuthTokenAsSession(sd *models.UserAuthToken, context context.Context, manager *scs.SessionManager) {
	manager.Put(context, models.SessionAccessTokenKey, sd.AccessToken.Value)
	manager.Put(context, models.SessionAccessTokenExpirationDateKey, sd.AccessToken.ExpirationDate)
	manager.Put(context, models.SessionRefreshTokenKey, sd.RefreshToken.Value)
	manager.Put(context, models.SessionRefreshTokenExpirationDateKey, sd.RefreshToken.ExpirationDate)
	manager.Put(context, models.SessionNetworkTypeKey, int8(sd.NetworkType))
	manager.Put(context, models.SessionUserDbIdKey, sd.UserDbId)
	manager.Put(context, models.SessionUserDeviceType, sd.UserDeviceType)
	manager.Put(context, models.SessionUserDeviceId, sd.UserDeviceId)
	manager.Put(context, models.SessionAppVersion, sd.AppVersion)
}
