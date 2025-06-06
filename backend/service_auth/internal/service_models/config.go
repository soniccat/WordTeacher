package service_models

type Configs struct {
	GoogleConfig   GoogleConfig
	VKIDConfig     VKIDConfig
	YandexIdConfig YandexIdConfig
}

type GoogleConfig struct {
	GoogleIdTokenAndroidAudience string `json:"googleIdTokenAndroidAudience"`
	GoogleIdTokenDesktopAudience string `json:"googleIdTokenDesktopAudience"`
}

type VKIDConfig struct {
	AccessToken string `json:"accessToken"`
}

type YandexIdConfig struct {
	ClientId string `json:"clientId"`
}
