package userauthtoken

import (
	"context"
	"github.com/alexedwards/scs/v2"
	"github.com/google/uuid"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models"
	"time"
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

func SaveUserAuthTokenAsSession(sd *models.UserAuthToken, context context.Context, manager *scs.SessionManager) {
	manager.Put(context, models.SessionAccessTokenKey, sd.AccessToken.Value)
	manager.Put(context, models.SessionAccessTokenExpirationDateKey, int64(sd.AccessToken.ExpirationDate))
	manager.Put(context, models.SessionRefreshTokenKey, sd.RefreshToken)
	manager.Put(context, models.SessionNetworkTypeKey, int8(sd.NetworkType))
	manager.Put(context, models.SessionUserMongoIdKey, sd.UserMongoId.Hex())
	manager.Put(context, models.SessionUserDeviceType, sd.UserDeviceType)
	manager.Put(context, models.SessionUserDeviceId, sd.UserDeviceId)
}
