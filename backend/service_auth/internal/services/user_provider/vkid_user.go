package user_provider

import (
	"context"
	"encoding/json"
	"errors"
	"models"
	"net/http"
	"net/url"
	"service_auth/internal/service_models"
	"time"
	"tools"
)

type VKSecureCheckTokenResponse struct {
	UserId  string `json:"user_id"`
	Success int    `json:"success"`
	Expire  int64  `json:"expire"`
}

func (h *Service) VKUser(
	context context.Context,
	token string,
	deviceType string,
) (*service_models.UserWithNetwork, error) {
	if deviceType != tools.DeviceTypeAndroid {
		return nil, errors.New("VKID is supported only in android")
	}

	url, e := url.Parse("https://api.vk.com/method/secure.checkToken")
	if e != nil {
		return nil, e
	}
	values := url.Query()
	values.Add("token", token)
	values.Add("v", "5.131")
	values.Add("access_token", "")
	url.RawQuery = values.Encode()

	r, e := http.NewRequest("GET", url.RequestURI(), nil)
	if e != nil {
		return nil, e
	}

	var response VKSecureCheckTokenResponse
	e = json.NewDecoder(r.Body).Decode(&response)
	if e != nil {
		return nil, e
	}

	if response.Success != 1 {
		return nil, errors.New("can't validate user")
	}

	if response.Expire != 0 && response.Expire <= time.Now().UTC().Unix() {
		return nil, errors.New("token is expired")
	}

	vkIDUser, err := h.userStorage.FindUserById(context, models.VKID, response.UserId)
	if err != nil {
		return nil, err
	}

	return &service_models.UserWithNetwork{
		User: vkIDUser,
		Network: models.UserNetwork{
			NetworkType:   models.VKID,
			NetworkUserId: response.UserId,
		},
	}, nil
}
