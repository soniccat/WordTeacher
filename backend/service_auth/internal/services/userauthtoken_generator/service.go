package userauthtoken_generator

import (
	"encoding/gob"
	"time"

	"github.com/alexedwards/scs/v2"

	"service_auth/internal/storage"
)

type Service struct {
	userRepository *storage.UserRepository
	sessionManager *scs.SessionManager
}

func NewUserAuthTokenGenerator(
	userRepository *storage.UserRepository,
	sessionManager *scs.SessionManager,
) *Service {
	gob.Register(time.Time{})
	return &Service{
		userRepository: userRepository,
		sessionManager: sessionManager,
	}
}
