package userauthtoken

import (
	"auth/cmd/accesstoken"
	"auth/cmd/usernetwork"
	"github.com/google/uuid"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"time"
)

const AccessTokenTimeout = time.Hour

type UserAuthToken struct {
	ID           *primitive.ObjectID         `bson:"_id,omitempty"`
	UserId       *primitive.ObjectID         `bson:"userId,omitempty"`
	NetworkType  usernetwork.UserNetworkType `bson:"networkType,omitempty"`
	AccessToken  accesstoken.AccessToken     `bson:"accessToken,omitempty"`
	RefreshToken string                      `bson:"refreshToken,omitempty"`
	DeviceId     string                      `bson:"deviceId,omitempty"`
	// TODO: consider to add last usage date
}

func New(
	userId *primitive.ObjectID,
	networkType usernetwork.UserNetworkType,
	deviceId string,
) (*UserAuthToken, error) {
	accessTokenValue, err := uuid.NewRandom()
	if err != nil {
		return nil, err
	}

	refreshTokenValue, err := uuid.NewRandom()
	if err != nil {
		return nil, err
	}

	return &UserAuthToken{
		UserId:      userId,
		NetworkType: networkType,
		AccessToken: accesstoken.AccessToken{
			Value:          accessTokenValue.String(),
			ExpirationDate: primitive.NewDateTimeFromTime(time.Now().Add(AccessTokenTimeout)),
		},
		RefreshToken: refreshTokenValue.String(),
		DeviceId:     deviceId,
	}, nil
}
