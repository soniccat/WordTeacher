package user_provider

import (
	"net/http"
	"service_auth/internal/service_models"
	"tools/logger"
)

type Service struct {
	logger         *logger.Logger
	googleConfig   service_models.GoogleConfig
	vkIdConfig     service_models.VKIDConfig
	yandexIdConfig service_models.YandexIdConfig
	userStorage    userStorage
	httpClient     http.Client
}

func New(
	logger *logger.Logger,
	googleConfig service_models.GoogleConfig,
	vkIdConfig service_models.VKIDConfig,
	yandexIdConfig service_models.YandexIdConfig,
	userStorage userStorage,
) *Service {

	httpClient := http.Client{}
	return &Service{
		logger:         logger,
		googleConfig:   googleConfig,
		vkIdConfig:     vkIdConfig,
		yandexIdConfig: yandexIdConfig,
		userStorage:    userStorage,
		httpClient:     httpClient,
	}
}
