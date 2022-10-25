package main

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
)

const SessionAccessTokenKey = "accessToken"
const SessionUserIdKey = "userId"

// Networks

type SocialCredentials struct {
	Token string `json:"token,omitempty"`
}

// Database

// collections
const (
	MongoDatabaseUsers         = "users"
	MongoCollectionUsers       = "users"
	MongoCollectionUserCounter = "counter"
	MongoCollectionAuthTokens  = "authTokens"
)

type UserCounter struct {
	Count uint64 `bson:"count"`
}

type User struct {
	ID       primitive.ObjectID `bson:"_id,omitempty"`
	Counter  uint64             `bson:"counter"`
	Networks []UserNetwork      `bson:"networks"`
}

type UserNetworkType int32

const (
	NetworkGoogle UserNetworkType = 1
)

type UserNetwork struct {
	ID            primitive.ObjectID `bson:"_id,omitempty"`
	NetworkType   UserNetworkType    `bson:"type"`
	NetworkUserId string             `bson:"networkUserId,omitempty"`
	Token         string             `bson:"token"`
	Email         string             `bson:"email,omitempty"`
}

type UserAuthToken struct {
	ID           primitive.ObjectID `bson:"_id,omitempty"`
	UserId       primitive.ObjectID `bson:"userId,omitempty"`
	NetworkId    primitive.ObjectID `bson:"networkId,omitempty"`
	AccessToken  AccessToken        `bson:"accessToken,omitempty"`
	RefreshToken string             `bson:"refreshToken,omitempty"`
}

type AccessToken struct {
	Value          string             `bson:"value"`
	ExpirationDate primitive.DateTime `bson:"expirationDate"`
}
