package auth

import (
	"context"
	serviceModels "service_auth/internal/models"
)

type authorizer interface {
	Authorize(ctx context.Context, userInfo serviceModels.UserInfo) (*serviceModels.AuthorizedUser, error)
}
