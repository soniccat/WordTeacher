package authorizer

import (
	"context"
	"models"
	serviceModels "service_auth/internal/models"
)

func (s *Service) Authorize(ctx context.Context, userInfo serviceModels.UserInfo) (*serviceModels.AuthorizedUser, error) {
	userWithNetwork, err := s.userResolver.ResolveUser(ctx, userInfo.NetworkType, userInfo.Token, userInfo.DeviceType)
	if err != nil {
		return nil, err
	}

	if userWithNetwork.User == nil { // Create a new user
		newUser, err := s.userStorage.InsertUser(
			ctx,
			&models.User{
				Networks: []models.UserNetwork{userWithNetwork.Network},
			},
		)
		if err != nil {
			return nil, err
		}

		userWithNetwork.User = newUser
	}

	// Create new access token / refresh token pair
	token, err := s.authTokenGenerator.Generate(
		ctx,
		userWithNetwork.User.Id.Hex(),
		userWithNetwork.Network.NetworkType,
		userInfo.DeviceType,
		userInfo.DeviceId,
	)
	if err != nil {
		return nil, err
	}

	return &serviceModels.AuthorizedUser{
		Token:       token,
		NetworkType: userInfo.NetworkType,
		DeviceType:  userInfo.DeviceType,
		DeviceId:    userInfo.DeviceId,
	}, nil
}
