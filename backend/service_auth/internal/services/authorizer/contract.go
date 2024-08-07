package authorizer

import (
	"context"
	"models"
	"service_auth/internal/service_models"
)

type userResolver interface {
	ResolveUser(
		ctx context.Context,
		networkType models.UserNetworkType,
		token string,
		deviceType string,
	) (*service_models.UserWithNetwork, error)
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
		appVersion string,
	) (*models.UserAuthToken, error)
}
