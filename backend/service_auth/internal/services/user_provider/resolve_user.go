package user_provider

import (
	"context"
	"fmt"
	"models"
	serviceModels "service_auth/internal/models"
)

func (s *Service) ResolveUser(
	ctx context.Context,
	networkType models.UserNetworkType,
	token string,
	deviceType string,
) (*serviceModels.UserWithNetwork, error) {

	var userWithNetwork *serviceModels.UserWithNetwork
	var err error
	if networkType == models.Google {
		userWithNetwork, err = s.GoogleUser(ctx, token, deviceType)
	} else if networkType == models.VKID {
		userWithNetwork, err = s.VKUser(ctx, token, deviceType)
	} else {
		return nil, fmt.Errorf("unsupported networkType: %d", networkType)
	}

	return userWithNetwork, err
}
