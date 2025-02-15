package user_provider

import (
	"context"
	"encoding/json"
	"models"
	"net/http"
	"net/url"
	"service_auth/internal/service_models"
	"strconv"
	"time"
	"tools"
	"tools/logger"
)

type VKSecureCheckTokenBody struct {
	Response VKSecureCheckTokenResponse `json:"response"`
}

type VKSecureCheckTokenResponse struct {
	UserId  int64 `json:"user_id"`
	Success int   `json:"success"`
	Expire  int64 `json:"expire"`
}

func (s *Service) VKUser(
	ctx context.Context,
	token string,
	deviceType string,
) (*service_models.UserWithNetwork, error) {
	if deviceType != tools.DeviceTypeAndroid {
		return nil, logger.Error(ctx, "VKID is supported only in android")
	}

	url, err := url.Parse("https://api.vk.com/method/secure.checkToken")
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}
	values := url.Query()
	values.Add("token", token)
	values.Add("v", "5.199")
	values.Add("access_token", s.vkIdConfig.AccessToken)
	url.RawQuery = values.Encode()

	r, err := http.NewRequest("GET", url.String(), nil)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}
	requestResponse, err := s.httpClient.Do(r)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	var resposeBody VKSecureCheckTokenBody
	err = json.NewDecoder(requestResponse.Body).Decode(&resposeBody)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	if resposeBody.Response.Success != 1 {
		ctx := logger.WrapContext(ctx, "token", token)
		return nil, logger.Error(ctx, "can't validate user")
	}

	if resposeBody.Response.Expire != 0 && resposeBody.Response.Expire <= time.Now().UTC().Unix() {
		return nil, logger.Error(ctx, "token is expired")
	}

	userIdAsString := strconv.FormatInt(resposeBody.Response.UserId, 10)
	vkIDUser, err := s.userStorage.FindUserById(ctx, models.VKID, userIdAsString)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	s.logger.Info(ctx, "vk user found", "token", token)

	return &service_models.UserWithNetwork{
		User: vkIDUser,
		Network: models.UserNetwork{
			NetworkType:   models.VKID,
			NetworkUserId: userIdAsString,
		},
	}, nil
}
