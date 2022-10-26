package accesstoken

import "go.mongodb.org/mongo-driver/bson/primitive"

type AccessToken struct {
	Value          string             `bson:"value"`
	ExpirationDate primitive.DateTime `bson:"expirationDate"`
}
