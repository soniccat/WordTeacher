package user_provider

import (
	"context"
	"encoding/json"
	"models"
	"tools/logger"

	"github.com/MicahParks/keyfunc/v3"
	"github.com/golang-jwt/jwt/v5"

	"service_auth/internal/service_models"
)

const (
	telegramIssuer = "https://oauth.telegram.org"
)

func (s *Service) TelegramUser(
	ctx context.Context,
	token string,
	deviceType string,
) (*service_models.UserWithNetwork, error) {

	// Uncomment if the link isn't blocked in your region
	// k, err := keyfunc.NewDefaultCtx(ctx, []string{"https://oauth.telegram.org/.well-known/jwks.json"}) // Context is used to end the refresh goroutine.
	// if err != nil {
	// 	return nil, logger.WrapError(ctx, err)
	// }

	// secp256k1 is removed because of parsing error
	jwksJSON := json.RawMessage(`{"keys":[{"alg":"RS256","e":"AQAB","ext":true,"key_ops":["verify"],"kty":"RSA","n":"5RneLtsKvVcxdv6gu6gxEQu30Cru5NiMQnY6SNr9ZyZFZ4ya-pfHNuaZXJ6QPG0JSFwoxeOkEO2-eZN_REVPm448PvjjsR1eQdZ5QpEkNxnItFcmxkHH91v5cgf52_EI9BGO-MT6f1vaBSg3uWHFlDxI7J2AYxNvd1_Nf3TkgrrR7gyJFTmEIai5RefGnA0KGNYDlRIGUzrz2F05n6gTaHFT_iHL5UHatTZA4GCiUSjIOuwqu5pE5uZge20TFv3cxXMQaFw_xv1pgQt_Rq8eoCN7TS0RQ0zjWKiad-W286BcFectXsUm03p5Nq_kY4mf_7rqwX_B8yy_bBreyKn7RQ","kid":"oidc-1"},{"alg":"ES256","kty":"EC","x":"ahVYrohhX6YA7w0P2gUNSwMFbaabCgBZFkeq9bWdmwU","y":"Ea8nKJ34VQMA7zv8aYDfzcBhXEjnWQ9C06jVke_eUV0","crv":"P-256","kid":"oidc-es256-1","use":"sig"},{"alg":"EdDSA","crv":"Ed25519","x":"i6BEafXMEe4osXgUTffpKAm6Cn6F2bhqPZoclunTAV4","kty":"OKP","kid":"oidc-eddsa-1","use":"sig"}]}`)
	k, err := keyfunc.NewJWKSetJSON(jwksJSON)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	// checks according to https://core.telegram.org/bots/telegram-login#validating-id-tokens
	t, err := jwt.Parse(token, k.Keyfunc,
		jwt.WithExpirationRequired(),
		jwt.WithAudience(s.telegramConfig.ClientId),
		jwt.WithIssuer(telegramIssuer))
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	userId, err := t.Claims.GetSubject()
	if err != nil {
		return nil, service_models.NewErrorInvalidToken(logger.Error(ctx, "Telegram Token doesn't have a sub"))
	}

	user, err := s.userStorage.FindUserById(ctx, models.Telegram, userId)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	return &service_models.UserWithNetwork{
		User: user,
		Network: models.UserNetwork{
			NetworkType:   models.Telegram,
			NetworkUserId: userId,
		},
	}, nil
}
