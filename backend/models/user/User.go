package user

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/usernetwork"
)

type UserCounter struct {
	Count uint64 `bson:"count"`
}

type User struct {
	ID       primitive.ObjectID        `bson:"_id,omitempty"`
	Counter  uint64                    `bson:"counter"`
	Networks []usernetwork.UserNetwork `bson:"networks"`
}
