package user_provider

import (
	"context"
	"models"
)

type userStorage interface {
	FindUserById(context context.Context, networkType models.UserNetworkType, userId string) (*models.User, error)
}
