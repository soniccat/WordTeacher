package userauthtoken

import (
	"models"
	"time"

	"github.com/google/uuid"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

func GenerateUserAuthToken(
	userId *primitive.ObjectID,
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
		UserMongoId: userId,
		NetworkType: networkType,
		AccessToken: models.AccessToken{
			Value:          accessTokenValue.String(),
			ExpirationDate: primitive.NewDateTimeFromTime(time.Now().Add(models.AccessTokenTimeout)),
		},
		RefreshToken:   refreshTokenValue.String(),
		UserDeviceType: deviceType,
		UserDeviceId:   deviceId,
	}, nil
}
