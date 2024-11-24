package user_provider

import (
	"context"
	"fmt"
	"models"
	"service_auth/internal/service_models"
	"tools/logger"
)

func (s *Service) ResolveUser(
	ctx context.Context,
	networkType models.UserNetworkType,
	token string,
	deviceType string,
) (*service_models.UserWithNetwork, error) {

	var userWithNetwork *service_models.UserWithNetwork
	var err error
	if networkType == models.Google {
		userWithNetwork, err = s.GoogleUser(ctx, token, deviceType)
	} else if networkType == models.VKID {
		userWithNetwork, err = s.VKUser(ctx, token, deviceType)
	} else if networkType == models.YandexId {
		userWithNetwork, err = s.YandexUser(ctx, token, deviceType)
	} else {
		err = logger.WrapError(ctx, fmt.Errorf("unsupported networkType: %d", networkType))
	}

	return userWithNetwork, err
}
