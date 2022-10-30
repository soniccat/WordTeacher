package user

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/usernetwork"
)

type User struct {
	ID       primitive.ObjectID        `bson:"_id,omitempty"`
	Networks []usernetwork.UserNetwork `bson:"networks"`
}
