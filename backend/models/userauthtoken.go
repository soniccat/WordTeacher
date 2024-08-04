package models

import (
	"context"
	"net/http"
	"time"
	"tools"
	"tools/logger"

	"github.com/alexedwards/scs/v2"
)

const AccessTokenTimeout = time.Hour
const RefreshTokenTimeout = 7 * 24 * time.Hour

const (
	SessionAccessTokenKey                = "accessToken"
	SessionAccessTokenExpirationDateKey  = "accessTokenExpirationDate"
	SessionRefreshTokenKey               = "refreshToken"
	SessionRefreshTokenExpirationDateKey = "refreshTokenExpirationDate"
	SessionNetworkTypeKey                = "networkType"
	SessionUserDbIdKey                   = "userDbId"
	SessionUserDeviceType                = "deviceType"
	SessionUserDeviceId                  = "deviceId"
	SessionAppVersion                    = "appVersion"
)

type UserAuthToken struct {
	Id             string          `bson:"_id,omitempty"`
	UserDbId       string          `bson:"userId,omitempty"`
	NetworkType    UserNetworkType `bson:"networkType,omitempty"`
	AccessToken    AccessToken     `bson:"accessToken,omitempty"`
	RefreshToken   RefreshToken    `bson:"refreshToken,omitempty"`
	UserDeviceType string          `bson:"deviceType,omitempty"`
	UserDeviceId   string          `bson:"deviceId,omitempty"`
	AppVersion     string          `bson:"version,omitempty"`
	// TODO: consider to add last usage date
}

func New(
	accessToken *AccessToken,
	refreshToken *RefreshToken,
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

	sessionRefreshTokenExpirationDate, ok := manager.Get(ctx, SessionRefreshTokenExpirationDateKey).(time.Time)
	if !ok {
		return nil, logger.Error(ctx, "session refresh token expiration date is missing")
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
		RefreshToken: RefreshToken{
			Value:          sessionRefreshToken,
			ExpirationDate: sessionRefreshTokenExpirationDate,
		},
		NetworkType:    UserNetworkType(networkType),
		UserDeviceType: sessionDeviceType,
		UserDeviceId:   sessionDeviceId,
		UserDbId:       sessionUserDbId,
		AppVersion:     appVersion,
	}, nil
}

func (sd *UserAuthToken) IsAccessTokenValid() bool {
	return time.Now().Compare(sd.AccessToken.ExpirationDate) < 0
}

func (sd *UserAuthToken) IsRefreshTokenValid() bool {
	return time.Now().Compare(sd.RefreshToken.ExpirationDate) < 0
}

func (sd *UserAuthToken) Match(
	ctx context.Context,
	accessToken string,
	refreshToken *string,
	userDeviceType string,
	userDeviceId string,
) error {
	if sd.AccessToken.Value != accessToken {
		return logger.Error(
			logger.WrapContext(ctx, "inAccessToken", accessToken, "sessionAccessToken", sd.AccessToken.Value),
			"accessToken doesn't match",
		)
	}

	if refreshToken != nil {
		if !sd.IsRefreshTokenValid() {
			return logger.Error(ctx, "refreshToken isn't valid")
		}

		if sd.RefreshToken.Value != *refreshToken {
			return logger.Error(ctx, "refreshToken doesn't match")
		}
	}

	if sd.UserDeviceType != userDeviceType {
		return logger.Error(ctx, "userDeviceType doesn't match")
	}

	if sd.UserDeviceId != userDeviceId {
		return logger.Error(ctx, "userDeviceId doesn't match")
	}

	return nil
}

func LogParams(t *UserAuthToken, headers http.Header) []any {
	if t != nil {
		return []any{
			"networkType", int(t.NetworkType),
			"userId", t.UserDbId,
			"deviceId", t.UserDeviceId,
			"deviceType", t.UserDeviceType,
			"appVersion", t.AppVersion,
		}
	} else {
		var params []any
		var deviceId = headers.Get(tools.HeaderDeviceId)
		var deviceType = headers.Get(tools.HeaderDeviceType)
		var appVersion = headers.Get(tools.HeaderAppVersion)
		if len(deviceId) > 0 {
			params = append(params, "deviceId", deviceId)
		}
		if len(deviceType) > 0 {
			params = append(params, "deviceType", deviceType)
		}
		if len(appVersion) > 0 {
			params = append(params, "appVersion", appVersion)
		}
		return params
	}
}
