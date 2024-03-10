package user_provider

import (
	"net/http"
	"service_auth/internal/service_models"
)

type Service struct {
	googleConfig service_models.GoogleConfig
	vkIdConfig   service_models.VKIDConfig
	userStorage  userStorage
	httpClient   http.Client
}

func New(
	googleConfig service_models.GoogleConfig,
	vkIdConfig service_models.VKIDConfig,
	userStorage userStorage,
) *Service {

	httpClient := http.Client{}
	return &Service{
		googleConfig: googleConfig,
		vkIdConfig:   vkIdConfig,
		userStorage:  userStorage,
		httpClient:   httpClient,
	}
}
