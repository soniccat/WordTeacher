package main

import (
	"auth/cmd/usernetwork"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

const CookieSession = "session"

const HeaderDeviceId = "deviceId"

const (
	SessionAccessTokenKey               = "accessToken"
	SessionAccessTokenExpirationDateKey = "accessTokenExpirationDate"
	SessionRefreshTokenKey              = "refreshToken"
	SessionUserIdKey                    = "userId"
)

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
	ID       primitive.ObjectID        `bson:"_id,omitempty"`
	Counter  uint64                    `bson:"counter"`
	Networks []usernetwork.UserNetwork `bson:"networks"`
}
