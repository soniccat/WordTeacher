package user

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/usernetwork"
)

type User struct {
	Id       primitive.ObjectID        `bson:"_id,omitempty"`
	Networks []usernetwork.UserNetwork `bson:"networks"`
}
