module wordlist_to_dsl

go 1.25.4

require tools v0.0.1

require (
	github.com/klauspost/compress v1.19.2 // indirect
	github.com/xdg-go/pbkdf2 v1.0.0 // indirect
	github.com/xdg-go/scram v1.2.0 // indirect
	github.com/xdg-go/stringprep v1.0.4 // indirect
	github.com/youmark/pkcs8 v0.0.0-20240726163527-a2c0da244d78 // indirect
	golang.org/x/crypto v0.53.0 // indirect
	golang.org/x/sync v0.21.0 // indirect
	golang.org/x/text v0.39.0 // indirect
)

require (
	github.com/alexedwards/scs/redisstore v0.0.0-20251002162104-209de6e426de // indirect
	github.com/alexedwards/scs/v2 v2.9.0 // indirect
	github.com/gomodule/redigo v1.9.3 // indirect
	go.mongodb.org/mongo-driver v1.17.10 // indirect
	go.mongodb.org/mongo-driver/v2 v2.9.2
)

replace tools => ../../tools
