package models

import "models"

type AuthorizedUser struct {
	Token       *models.UserAuthToken
	NetworkType models.UserNetworkType
	DeviceType  string
	DeviceId    string
}
