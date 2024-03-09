package user_provider

type Service struct {
	userStorage userStorage
}

func New(userStorage userStorage) *Service {
	return &Service{
		userStorage: userStorage,
	}
}
