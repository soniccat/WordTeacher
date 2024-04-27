package service_models

import "models"

type UserInfo struct {
	NetworkType models.UserNetworkType
	DeviceType  string
	DeviceId    string
	AppVersion  string
}
