package models

type UserNetworkType int

const (
	Google UserNetworkType = iota
	VKID
)

type UserNetwork struct {
	NetworkType   UserNetworkType `bson:"type"`
	NetworkUserId string          `bson:"networkUserId,omitempty"`
	// Email         string          `bson:"email,omitempty"`
	// Name          string          `bson:"name,omitempty"`
}
