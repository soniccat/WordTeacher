package usernetwork

type UserNetworkType int32

const (
	Google UserNetworkType = 1
)

type UserNetwork struct {
	NetworkType   UserNetworkType `bson:"type"`
	NetworkUserId string          `bson:"networkUserId,omitempty"`
	Email         string          `bson:"email,omitempty"`
	Name          string          `bson:"name,omitempty"`
}
