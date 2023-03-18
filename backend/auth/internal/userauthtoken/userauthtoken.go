package userauthtoken

import (
	"context"
	"github.com/alexedwards/scs/v2"
	"github.com/google/uuid"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/accesstoken"
	"models/userauthtoken"
	"models/usernetwork"
	"time"
)

func GenerateUserAuthToken(
	userId *primitive.ObjectID,
	networkType usernetwork.UserNetworkType,
	deviceType string,
	deviceId string,
) (*userauthtoken.UserAuthToken, error) {
	accessTokenValue, err := uuid.NewRandom()
	if err != nil {
		return nil, err
	}

	refreshTokenValue, err := uuid.NewRandom()
	if err != nil {
		return nil, err
	}

	return &userauthtoken.UserAuthToken{
		UserMongoId: userId,
		NetworkType: networkType,
		AccessToken: accesstoken.AccessToken{
			Value:          accessTokenValue.String(),
			ExpirationDate: primitive.NewDateTimeFromTime(time.Now().Add(userauthtoken.AccessTokenTimeout)),
		},
		RefreshToken:   refreshTokenValue.String(),
		UserDeviceType: deviceType,
		UserDeviceId:   deviceId,
	}, nil
}

func SaveUserAuthTokenAsSession(sd *userauthtoken.UserAuthToken, context context.Context, manager *scs.SessionManager) {
	manager.Put(context, userauthtoken.SessionAccessTokenKey, sd.AccessToken.Value)
	manager.Put(context, userauthtoken.SessionAccessTokenExpirationDateKey, int64(sd.AccessToken.ExpirationDate))
	manager.Put(context, userauthtoken.SessionRefreshTokenKey, sd.RefreshToken)
	manager.Put(context, userauthtoken.SessionNetworkTypeKey, int8(sd.NetworkType))
	manager.Put(context, userauthtoken.SessionUserMongoIdKey, sd.UserMongoId.Hex())
	manager.Put(context, userauthtoken.SessionUserDeviceType, sd.UserDeviceType)
	manager.Put(context, userauthtoken.SessionUserDeviceId, sd.UserDeviceId)
}
