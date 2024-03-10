package token_refresher

import "github.com/alexedwards/scs/v2"

type Service struct {
	authTokenGenerator authTokenGenerator
	sessionManager     *scs.SessionManager
}

func New(
	authTokenGenerator authTokenGenerator,
	sessionManager *scs.SessionManager,
) *Service {
	return &Service{
		authTokenGenerator: authTokenGenerator,
		sessionManager:     sessionManager,
	}
}
