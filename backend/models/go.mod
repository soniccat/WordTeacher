module models

go 1.22.2

require tools v0.0.0

replace tools => ../tools

require (
	github.com/alexedwards/scs/v2 v2.5.1
	go.mongodb.org/mongo-driver v1.10.3
)

require (
	github.com/alexedwards/scs/redisstore v0.0.0-20230305153148-62e546ce9d2d // indirect
	github.com/gomodule/redigo v1.8.9 // indirect
)
