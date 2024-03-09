package models

import "models"

type UserInfo struct {
	NetworkType models.UserNetworkType
	Token       string
	DeviceType  string
	DeviceId    string
}
