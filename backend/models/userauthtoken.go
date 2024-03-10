package models

import (
	"context"
	"errors"
	"time"

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
)

type UserAuthToken struct {
	Id             string          `bson:"_id,omitempty"`
	UserDbId       string          `bson:"userId,omitempty"`
	NetworkType    UserNetworkType `bson:"networkType,omitempty"`
	AccessToken    AccessToken     `bson:"accessToken,omitempty"`
	RefreshToken   string          `bson:"refreshToken,omitempty"`
	UserDeviceType string          `bson:"deviceType,omitempty"`
	UserDeviceId   string          `bson:"deviceId,omitempty"`
	// TODO: consider to add last usage date
}

func New(
	accessToken *AccessToken,
	refreshToken *string,
	networkType UserNetworkType,
	userDeviceType string,
	userDeviceId string,
	userDbId string,
) *UserAuthToken {
	return &UserAuthToken{
		AccessToken:    *accessToken,
		RefreshToken:   *refreshToken,
		NetworkType:    networkType,
		UserDeviceType: userDeviceType,
		UserDeviceId:   userDeviceId,
		UserDbId:       userDbId,
	}
}

// TODO: replace *scs.SessionManager dep with an interface
func Load(context context.Context, manager *scs.SessionManager) (*UserAuthToken, error) {
	sessionAccessToken, ok := manager.Get(context, SessionAccessTokenKey).(string)
	if !ok {
		return nil, errors.New("session access token is missing")
	}

	sessionAccessTokenExpirationDate, ok := manager.Get(context, SessionAccessTokenExpirationDateKey).(time.Time)
	if !ok {
		return nil, errors.New("session access token expiration date is missing")
	}

	sessionRefreshToken, ok := manager.Get(context, SessionRefreshTokenKey).(string)
	if !ok {
		return nil, errors.New("session refresh token is missing")
	}

	networkType, ok := manager.Get(context, SessionNetworkTypeKey).(int8)
	if !ok {
		return nil, errors.New("session networkType is missing")
	}

	sessionDeviceType, ok := manager.Get(context, SessionUserDeviceType).(string)
	if !ok || len(sessionDeviceType) == 0 {
		return nil, errors.New("session device type is missing")
	}

	sessionDeviceId, ok := manager.Get(context, SessionUserDeviceId).(string)
	if !ok || len(sessionDeviceId) == 0 {
		return nil, errors.New("session device id is missing")
	}

	sessionUserDbId, ok := manager.Get(context, SessionUserDbIdKey).(string)
	if !ok {
		return nil, errors.New("session user mongo id is missing")
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
