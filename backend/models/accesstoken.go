package models

import "time"

type AccessToken struct {
	Value          string    `bson:"value"`
	ExpirationDate time.Time `bson:"expirationDate"`
}
