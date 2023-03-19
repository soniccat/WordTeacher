module service_cardsetsearch

go 1.19

require tools v0.0.0

replace tools => ../tools

require models v0.0.0

replace models => ../models

require (
	api v0.0.0
	github.com/alexedwards/scs/v2 v2.5.1
	github.com/gorilla/mux v1.8.0
	go.mongodb.org/mongo-driver v1.10.3
)

require (
	github.com/alexedwards/scs/redisstore v0.0.0-20230305153148-62e546ce9d2d // indirect
	github.com/golang/snappy v0.0.1 // indirect
	github.com/gomodule/redigo v1.8.9 // indirect
	github.com/klauspost/compress v1.13.6 // indirect
	github.com/montanaflynn/stats v0.0.0-20171201202039-1bf9dbcd8cbe // indirect
	github.com/pkg/errors v0.9.1 // indirect
	github.com/xdg-go/pbkdf2 v1.0.0 // indirect
	github.com/xdg-go/scram v1.1.1 // indirect
	github.com/xdg-go/stringprep v1.0.3 // indirect
	github.com/youmark/pkcs8 v0.0.0-20181117223130-1be2e3e5546d // indirect
	golang.org/x/crypto v0.0.0-20220622213112-05595931fe9d // indirect
	golang.org/x/sync v0.0.0-20210220032951-036812b2e83c // indirect
	golang.org/x/text v0.3.7 // indirect
)

replace api => ../api
