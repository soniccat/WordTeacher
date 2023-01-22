package apphelpers

import (
	"github.com/alexedwards/scs/redisstore"
	"github.com/alexedwards/scs/v2"
	"github.com/gomodule/redigo/redis"
	"time"
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
	sessionManager.Lifetime = 24 * time.Hour
	sessionManager.Cookie.Path = "/api/"
	return sessionManager
}
