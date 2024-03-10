package service_models

type Configs struct {
	GoogleConfig GoogleConfig
	VKIDConfig   VKIDConfig
}

type GoogleConfig struct {
	GoogleIdTokenAndroidAudience string `json:"googleIdTokenAndroidAudience"`
	GoogleIdTokenDesktopAudience string `json:"googleIdTokenDesktopAudience"`
}

type VKIDConfig struct {
	AccessToken string `json:"accessToken"`
}
