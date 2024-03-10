package service_models

import "models"

// user with a selected network
type UserWithNetwork struct {
	User    *models.User // nil - not an existing user, a new one
	Network models.UserNetwork
}
