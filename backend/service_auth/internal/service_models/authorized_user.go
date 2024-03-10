package service_models

import "models"

type AuthorizedUser struct {
	UserId      string
	Token       *models.UserAuthToken
	NetworkType models.UserNetworkType
	DeviceType  string
	DeviceId    string
}
