package userauthtoken_generator

import (
	"context"
	"models"

	"github.com/alexedwards/scs/v2"
	"go.mongodb.org/mongo-driver/bson/primitive"

	"service_auth/internal/storage"
)

type UserAuthTokenGenerator interface {
	Generate(
		context context.Context,
		userMongoId *primitive.ObjectID,
		networkType models.UserNetworkType,
		deviceType string,
		deviceId string,
	) (*models.UserAuthToken, error)
}

type userAuthTokenGenerator struct {
	userRepository *storage.UserRepository
	sessionManager *scs.SessionManager
}

func NewUserAuthTokenGenerator(
	userRepository *storage.UserRepository,
	sessionManager *scs.SessionManager,
) UserAuthTokenGenerator {
	return &userAuthTokenGenerator{
		userRepository: userRepository,
		sessionManager: sessionManager,
	}
}

func (g *userAuthTokenGenerator) Generate(
	context context.Context,
	userMongoId *primitive.ObjectID,
	networkType models.UserNetworkType,
	deviceType string,
	deviceId string,
) (*models.UserAuthToken, error) {
	token, err := g.userRepository.GenerateUserAuthToken(
		context,
		userMongoId,
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
	manager.Put(context, models.SessionAccessTokenExpirationDateKey, int64(sd.AccessToken.ExpirationDate))
	manager.Put(context, models.SessionRefreshTokenKey, sd.RefreshToken)
	manager.Put(context, models.SessionNetworkTypeKey, int8(sd.NetworkType))
	manager.Put(context, models.SessionUserMongoIdKey, sd.UserMongoId.Hex())
	manager.Put(context, models.SessionUserDeviceType, sd.UserDeviceType)
	manager.Put(context, models.SessionUserDeviceId, sd.UserDeviceId)
}
