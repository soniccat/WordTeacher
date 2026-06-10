package models

import "fmt"

type UserNetworkType int

const (
	Google UserNetworkType = iota
	VKID
	YandexId
	Telegram

	GoogleString   = "google"
	VKIDString     = "vkid"
	YandexIdString = "yandexid"
	TelegramString = "telegram"
)

type UserNetwork struct {
	NetworkType   UserNetworkType `bson:"type"`
	NetworkUserId string          `bson:"networkUserId"`
}

func UserNetworkTypeFromString(value string) (UserNetworkType, error) {
	switch value {
	case GoogleString:
		return Google, nil
	case VKIDString:
		return VKID, nil
	case YandexIdString:
		return YandexId, nil
	case TelegramString:
		return Telegram, nil
	}
	return Google, fmt.Errorf("invalid user network type (%s)", value)
}
