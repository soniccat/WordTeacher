package usernetwork

type UserNetworkType int

const (
	Google UserNetworkType = iota
)

type UserNetwork struct {
	NetworkType   UserNetworkType `bson:"type"`
	NetworkUserId string          `bson:"networkUserId,omitempty"`
	Email         string          `bson:"email,omitempty"`
	Name          string          `bson:"name,omitempty"`
}
