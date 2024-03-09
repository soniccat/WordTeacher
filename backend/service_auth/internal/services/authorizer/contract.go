package authorizer

import (
	"context"
	"models"
	serviceModels "service_auth/internal/models"
)

type userResolver interface {
	ResolveUser(
		ctx context.Context,
		networkType models.UserNetworkType,
		token string,
		deviceType string,
	) (*serviceModels.UserWithNetwork, error)
}

type userStorage interface {
	InsertUser(context context.Context, user *models.User) (*models.User, error)
}

type authTokenGenerator interface {
	Generate(
		context context.Context,
		userDbId string,
		networkType models.UserNetworkType,
		deviceType string,
		deviceId string,
	) (*models.UserAuthToken, error)
}
