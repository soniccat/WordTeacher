package authorizer

type Service struct {
	userResolver       userResolver
	userStorage        userStorage
	authTokenGenerator authTokenGenerator
}

func New(
	userResolver userResolver,
	userStorage userStorage,
	authTokenGenerator authTokenGenerator,
) *Service {
	return &Service{
		userResolver:       userResolver,
		userStorage:        userStorage,
		authTokenGenerator: authTokenGenerator,
	}
}
