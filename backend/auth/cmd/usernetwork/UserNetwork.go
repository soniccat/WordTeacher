package usernetwork

import "go.mongodb.org/mongo-driver/bson/primitive"

type UserNetworkType int32

const (
	Google UserNetworkType = 1
)

type UserNetwork struct {
	ID            primitive.ObjectID `bson:"_id,omitempty"`
	NetworkType   UserNetworkType    `bson:"type"`
	NetworkUserId string             `bson:"networkUserId,omitempty"`
	Email         string             `bson:"email,omitempty"`
}
