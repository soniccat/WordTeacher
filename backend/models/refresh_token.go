package models

import "time"

type RefreshToken struct {
	Value          string    `bson:"value"`
	ExpirationDate time.Time `bson:"expirationDate"`
}
