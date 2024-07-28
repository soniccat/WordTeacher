package tools

import (
	"time"

	"github.com/alexedwards/scs/redisstore"
	"github.com/alexedwards/scs/v2"
	"github.com/gomodule/redigo/redis"
)

func CreateSessionManager(redisAddress string) *scs.SessionManager {
	pool := &redis.Pool{
		MaxIdle: 10,
		Dial: func() (redis.Conn, error) {
			return redis.Dial("tcp", redisAddress)
		},
	}

	sessionManager := scs.New()
	sessionManager.Store = redisstore.New(pool)
	sessionManager.Lifetime = 8 * 24 * time.Hour // a bit more than refresh token expiration lifetime
	return sessionManager
}
