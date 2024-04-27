package models

import (
	"context"
	"time"
	"tools/logger"

	"github.com/alexedwards/scs/v2"
)

const AccessTokenTimeout = time.Hour // TODO: change to several days

const (
	SessionAccessTokenKey               = "accessToken"
	SessionAccessTokenExpirationDateKey = "accessTokenExpirationDate"
	SessionRefreshTokenKey              = "refreshToken"
	SessionNetworkTypeKey               = "networkType"
	SessionUserDbIdKey                  = "userDbId"
	SessionUserDeviceType               = "deviceType"
	SessionUserDeviceId                 = "deviceId"
	SessionAppVersion                   = "appVersion"
)

type UserAuthToken struct {
	Id             string          `bson:"_id,omitempty"`
	UserDbId       string          `bson:"userId,omitempty"`
	NetworkType    UserNetworkType `bson:"networkType,omitempty"`
	AccessToken    AccessToken     `bson:"accessToken,omitempty"`
	RefreshToken   string          `bson:"refreshToken,omitempty"`
	UserDeviceType string          `bson:"deviceType,omitempty"`
	UserDeviceId   string          `bson:"deviceId,omitempty"`
	AppVersion     string          `bson:"version,omitempty"`
	// TODO: consider to add last usage date
}

func New(
	accessToken *AccessToken,
	refreshToken *string,
	networkType UserNetworkType,
	userDeviceType string,
	userDeviceId string,
	userDbId string,
	appVersion string,
) *UserAuthToken {
	return &UserAuthToken{
		AccessToken:    *accessToken,
		RefreshToken:   *refreshToken,
		NetworkType:    networkType,
		UserDeviceType: userDeviceType,
		UserDeviceId:   userDeviceId,
		UserDbId:       userDbId,
		AppVersion:     appVersion,
	}
}

// TODO: replace *scs.SessionManager dep with an interface
func Load(ctx context.Context, manager *scs.SessionManager) (*UserAuthToken, error) {
	sessionAccessToken, ok := manager.Get(ctx, SessionAccessTokenKey).(string)
	if !ok {
		return nil, logger.Error(ctx, "session access token is missing")
	}

	sessionAccessTokenExpirationDate, ok := manager.Get(ctx, SessionAccessTokenExpirationDateKey).(time.Time)
	if !ok {
		return nil, logger.Error(ctx, "session access token expiration date is missing")
	}

	sessionRefreshToken, ok := manager.Get(ctx, SessionRefreshTokenKey).(string)
	if !ok {
		return nil, logger.Error(ctx, "session refresh token is missing")
	}

	networkType, ok := manager.Get(ctx, SessionNetworkTypeKey).(int8)
	if !ok {
		return nil, logger.Error(ctx, "session networkType is missing")
	}

	sessionDeviceType, ok := manager.Get(ctx, SessionUserDeviceType).(string)
	if !ok || len(sessionDeviceType) == 0 {
		return nil, logger.Error(ctx, "session device type is missing")
	}

	sessionDeviceId, ok := manager.Get(ctx, SessionUserDeviceId).(string)
	if !ok || len(sessionDeviceId) == 0 {
		return nil, logger.Error(ctx, "session device id is missing")
	}

	sessionUserDbId, ok := manager.Get(ctx, SessionUserDbIdKey).(string)
	if !ok {
		return nil, logger.Error(ctx, "session user mongo id is missing")
	}

	appVersion, ok := manager.Get(ctx, SessionAppVersion).(string)
	if !ok {
		return nil, logger.Error(ctx, "session app version is missing")
	}

	return &UserAuthToken{
		AccessToken: AccessToken{
			Value:          sessionAccessToken,
			ExpirationDate: sessionAccessTokenExpirationDate,
		},
		RefreshToken:   sessionRefreshToken,
		NetworkType:    UserNetworkType(networkType),
		UserDeviceType: sessionDeviceType,
		UserDeviceId:   sessionDeviceId,
		UserDbId:       sessionUserDbId,
		AppVersion:     appVersion,
	}, nil
}

func (sd *UserAuthToken) IsValid() bool {
	return time.Now().Compare(sd.AccessToken.ExpirationDate) < 0
}

func (sd *UserAuthToken) IsMatched(
	accessToken string,
	refreshToken *string,
	userDeviceType string,
	userDeviceId string,
) bool {
	return sd.AccessToken.Value == accessToken &&
		(refreshToken == nil || sd.RefreshToken == *refreshToken) &&
		sd.UserDeviceType == userDeviceType &&
		sd.UserDeviceId == userDeviceId
}

func (sd *UserAuthToken) LogParams() []any {
	return []any{
		"networkType", int(sd.NetworkType),
		"userId", sd.UserDbId,
		"deviceId", sd.UserDeviceId,
		"deviceType", sd.UserDeviceType,
		"appVersion", sd.AppVersion,
	}
}
