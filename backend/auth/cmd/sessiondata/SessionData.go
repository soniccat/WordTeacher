package sessiondata

import (
	"auth/cmd/accesstoken"
	"auth/cmd/usernetwork"
	"context"
	"errors"
	"github.com/alexedwards/scs/v2"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"time"
)

const (
	SessionAccessTokenKey               = "accessToken"
	SessionAccessTokenExpirationDateKey = "accessTokenExpirationDate"
	SessionRefreshTokenKey              = "refreshToken"
	SessionNetworkTypeKey               = "networkType"
	SessionUserMongoIdKey               = "userMongoId"
	SessionUserDeviceId                 = "deviceId"
)

type SessionData struct {
	AccessToken  accesstoken.AccessToken     `json:"accessToken,omitempty"`
	RefreshToken string                      `json:"refreshToken,omitempty"`
	NetworkType  usernetwork.UserNetworkType `json:"networkType,omitempty"`
	UserDeviceId string                      `json:"userDeviceId,omitempty"`
	UserMongoId  primitive.ObjectID          `json:"userMongoId,omitempty"`
}

func New(
	accessToken *accesstoken.AccessToken,
	refreshToken *string,
	networkType usernetwork.UserNetworkType,
	userDeviceId *string,
	userMongoId *primitive.ObjectID,
) *SessionData {
	return &SessionData{
		AccessToken:  *accessToken,
		RefreshToken: *refreshToken,
		NetworkType:  networkType,
		UserDeviceId: *userDeviceId,
		UserMongoId:  *userMongoId,
	}
}

func (sd *SessionData) Save(context context.Context, manager *scs.SessionManager) {
	manager.Put(context, SessionAccessTokenKey, sd.AccessToken.Value)
	manager.Put(context, SessionAccessTokenExpirationDateKey, int64(sd.AccessToken.ExpirationDate))
	manager.Put(context, SessionRefreshTokenKey, sd.RefreshToken)
	manager.Put(context, SessionNetworkTypeKey, int32(sd.NetworkType))
	manager.Put(context, SessionUserMongoIdKey, sd.UserMongoId.Hex())
	manager.Put(context, SessionUserDeviceId, sd.UserDeviceId)
}

func Load(context context.Context, manager *scs.SessionManager) (*SessionData, error) {
	sessionAccessToken, ok := manager.Get(context, SessionAccessTokenKey).(string)
	if !ok {
		return nil, errors.New("access token is missing")
	}

	sessionAccessTokenExpirationDate, ok := manager.Get(context, SessionAccessTokenExpirationDateKey).(int64)
	if !ok {
		return nil, errors.New("access token expiration date is missing")
	}

	sessionRefreshToken, ok := manager.Get(context, SessionRefreshTokenKey).(string)
	if !ok {
		return nil, errors.New("refresh token is missing")
	}

	networkType, ok := manager.Get(context, SessionNetworkTypeKey).(int32)
	if !ok {
		return nil, errors.New("networkType is missing")
	}

	sessionDeviceId, ok := manager.Get(context, SessionUserDeviceId).(string)
	if !ok || len(sessionDeviceId) == 0 {
		return nil, errors.New("device id is missing")
	}

	sessionUserMongoIdHex, ok := manager.Get(context, SessionUserMongoIdKey).(string)
	if !ok {
		return nil, errors.New("user mongo id is missing")
	}

	sessionUserMongoId, err := primitive.ObjectIDFromHex(sessionUserMongoIdHex)
	if err != nil {
		return nil, errors.New("user mongo id is missing")
	}

	return &SessionData{
		AccessToken: accesstoken.AccessToken{
			Value:          sessionAccessToken,
			ExpirationDate: primitive.DateTime(sessionAccessTokenExpirationDate),
		},
		RefreshToken: sessionRefreshToken,
		NetworkType:  usernetwork.UserNetworkType(networkType),
		UserDeviceId: sessionDeviceId,
		UserMongoId:  sessionUserMongoId,
	}, nil
}

func (sd *SessionData) IsValid() bool {
	return primitive.NewDateTimeFromTime(time.Now()) < sd.AccessToken.ExpirationDate
}

func (sd *SessionData) IsMatched(
	accessToken string,
	refreshToken string,
	userDeviceId string,
) bool {
	return sd.AccessToken.Value == accessToken &&
		sd.RefreshToken == refreshToken &&
		sd.UserDeviceId == userDeviceId
}
