package token_refresher

import (
	"context"
	"models"
)

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
