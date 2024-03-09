package userauthtoken_generator

import (
	"context"
	"encoding/gob"
	"models"
	"time"

	"github.com/alexedwards/scs/v2"

	"service_auth/internal/storage"
)

type Service struct {
	userRepository *storage.UserRepository
	sessionManager *scs.SessionManager
}

func NewUserAuthTokenGenerator(
	userRepository *storage.UserRepository,
	sessionManager *scs.SessionManager,
) *Service {
	gob.Register(time.Time{})
	return &Service{
		userRepository: userRepository,
		sessionManager: sessionManager,
	}
}

func (g *Service) Generate(
	context context.Context,
	userDbId string,
	networkType models.UserNetworkType,
	deviceType string,
	deviceId string,
) (*models.UserAuthToken, error) {
	token, err := g.userRepository.GenerateUserAuthToken(
		context,
		userDbId,
		networkType,
		deviceType,
		deviceId,
	)
	if err != nil {
		return nil, err
	}

	saveUserAuthTokenAsSession(token, context, g.sessionManager)
	return token, nil
}

func saveUserAuthTokenAsSession(sd *models.UserAuthToken, context context.Context, manager *scs.SessionManager) {
	manager.Put(context, models.SessionAccessTokenKey, sd.AccessToken.Value)
	manager.Put(context, models.SessionAccessTokenExpirationDateKey, sd.AccessToken.ExpirationDate)
	manager.Put(context, models.SessionRefreshTokenKey, sd.RefreshToken)
	manager.Put(context, models.SessionNetworkTypeKey, int8(sd.NetworkType))
	manager.Put(context, models.SessionUserDbIdKey, sd.UserDbId)
	manager.Put(context, models.SessionUserDeviceType, sd.UserDeviceType)
	manager.Put(context, models.SessionUserDeviceId, sd.UserDeviceId)
}
