package refresh

import (
	"context"
	"service_auth/internal/service_models"
)

type tokenRefresher interface {
	RefreshToken(
		ctx context.Context,
		userTokens service_models.UserTokens,
		userInfo service_models.UserInfo,
	) (*service_models.AuthorizedUser, error)
}
