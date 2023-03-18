package userauthtoken

import (
	"context"
	"errors"
	"github.com/alexedwards/scs/v2"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/accesstoken"
	"models/usernetwork"
	"time"
)

const AccessTokenTimeout = time.Hour // TODO: change to several days

const (
	SessionAccessTokenKey               = "accessToken"
	SessionAccessTokenExpirationDateKey = "accessTokenExpirationDate"
	SessionRefreshTokenKey              = "refreshToken"
	SessionNetworkTypeKey               = "networkType"
	SessionUserMongoIdKey               = "userMongoId"
	SessionUserDeviceType               = "deviceType"
	SessionUserDeviceId                 = "deviceId"
)

type UserAuthToken struct {
	Id             *primitive.ObjectID         `bson:"_id,omitempty"`
	UserMongoId    *primitive.ObjectID         `bson:"userId,omitempty"`
	NetworkType    usernetwork.UserNetworkType `bson:"networkType,omitempty"`
	AccessToken    accesstoken.AccessToken     `bson:"accessToken,omitempty"`
	RefreshToken   string                      `bson:"refreshToken,omitempty"`
	UserDeviceType string                      `bson:"deviceType,omitempty"`
	UserDeviceId   string                      `bson:"deviceId,omitempty"`
	// TODO: consider to add last usage date
}

func New(
	accessToken *accesstoken.AccessToken,
	refreshToken *string,
	networkType usernetwork.UserNetworkType,
	userDeviceType string,
	userDeviceId string,
	userMongoId *primitive.ObjectID,
) *UserAuthToken {
	return &UserAuthToken{
		AccessToken:    *accessToken,
		RefreshToken:   *refreshToken,
		NetworkType:    networkType,
		UserDeviceType: userDeviceType,
		UserDeviceId:   userDeviceId,
		UserMongoId:    userMongoId,
	}
}

func Load(context context.Context, manager *scs.SessionManager) (*UserAuthToken, error) {
	sessionAccessToken, ok := manager.Get(context, SessionAccessTokenKey).(string)
	if !ok {
		return nil, errors.New("session access token is missing")
	}

	sessionAccessTokenExpirationDate, ok := manager.Get(context, SessionAccessTokenExpirationDateKey).(int64)
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

	sessionUserMongoIdHex, ok := manager.Get(context, SessionUserMongoIdKey).(string)
	if !ok {
		return nil, errors.New("session user mongo id is missing")
	}

	sessionUserMongoId, err := primitive.ObjectIDFromHex(sessionUserMongoIdHex)
	if err != nil {
		return nil, err
	}

	return &UserAuthToken{
		AccessToken: accesstoken.AccessToken{
			Value:          sessionAccessToken,
			ExpirationDate: primitive.DateTime(sessionAccessTokenExpirationDate),
		},
		RefreshToken:   sessionRefreshToken,
		NetworkType:    usernetwork.UserNetworkType(networkType),
		UserDeviceType: sessionDeviceType,
		UserDeviceId:   sessionDeviceId,
		UserMongoId:    &sessionUserMongoId,
	}, nil
}

func (sd *UserAuthToken) IsValid() bool {
	return primitive.NewDateTimeFromTime(time.Now()) < sd.AccessToken.ExpirationDate
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
