package models

import "fmt"

type UserNetworkType int

const (
	Google UserNetworkType = iota
	VKID

	GoogleString = "google"
	VKIDString   = "vkid"
)

type UserNetwork struct {
	NetworkType   UserNetworkType `bson:"type"`
	NetworkUserId string          `bson:"networkUserId,omitempty"`
}

func UserNetworkTypeFromString(value string) (UserNetworkType, error) {
	switch value {
	case GoogleString:
		return Google, nil
	}

	if value == GoogleString {
		return Google, nil
	} else if value == VKIDString {
		return VKID, nil
	}

	return Google, fmt.Errorf("invalid user network type (%s)", value)
}
