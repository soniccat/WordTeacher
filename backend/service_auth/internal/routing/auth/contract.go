package auth

import (
	"context"
	"service_auth/internal/service_models"
)

type authorizer interface {
	Authorize(
		ctx context.Context,
		networkToken string,
		userInfo service_models.UserInfo,
	) (*service_models.AuthorizedUser, error)
}
