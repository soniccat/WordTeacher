package user_provider

import (
	"context"
	"encoding/json"
	"models"
	"net/http"
	"net/url"
	"service_auth/internal/service_models"
	"tools"
	"tools/logger"
)

type YandexInfoBody struct {
	Id       string `json:"id"`
	ClientId string `json:"client_id"`
}

func (s *Service) YandexUser(
	ctx context.Context,
	token string,
	deviceType string,
) (*service_models.UserWithNetwork, error) {
	if deviceType != tools.DeviceTypeAndroid {
		return nil, logger.Error(ctx, "YandexId is supported only in android")
	}

	url, err := url.Parse("https://login.yandex.ru/info")
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}
	values := url.Query()
	values.Add("format", "json")
	url.RawQuery = values.Encode()

	r, err := http.NewRequest("GET", url.String(), nil)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}
	r.Header.Add("Authorization", "OAuth "+token)

	requestResponse, err := s.httpClient.Do(r)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	var resposeBody YandexInfoBody
	err = json.NewDecoder(requestResponse.Body).Decode(&resposeBody)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	if resposeBody.ClientId != s.yandexIdConfig.ClientId {
		ctx := logger.WrapContext(ctx, "token", token)
		return nil, logger.Error(ctx, "clientId is invalid")
	}

	yandexIdUser, err := s.userStorage.FindUserById(ctx, models.YandexId, resposeBody.Id)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	return &service_models.UserWithNetwork{
		User: yandexIdUser,
		Network: models.UserNetwork{
			NetworkType:   models.YandexId,
			NetworkUserId: resposeBody.Id,
		},
	}, nil
}
