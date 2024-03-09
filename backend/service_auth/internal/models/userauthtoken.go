package models

import (
	"models"
	"time"

	"github.com/google/uuid"
)

func GenerateUserAuthToken(
	userDbId string,
	networkType models.UserNetworkType,
	deviceType string,
	deviceId string,
) (*models.UserAuthToken, error) {
	accessTokenValue, err := uuid.NewRandom()
	if err != nil {
		return nil, err
	}

	refreshTokenValue, err := uuid.NewRandom()
	if err != nil {
		return nil, err
	}

	return &models.UserAuthToken{
		UserDbId:    userDbId,
		NetworkType: networkType,
		AccessToken: models.AccessToken{
			Value:          accessTokenValue.String(),
			ExpirationDate: time.Now().Add(models.AccessTokenTimeout),
		},
		RefreshToken:   refreshTokenValue.String(),
		UserDeviceType: deviceType,
		UserDeviceId:   deviceId,
	}, nil
}
