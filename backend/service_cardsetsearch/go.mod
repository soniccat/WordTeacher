module service_cardsetsearch

go 1.22.2

require tools v0.0.0

replace tools => ../tools

require models v0.0.0

replace models => ../models

require service_cardsets v0.0.0

replace service_cardsets => ../service_cardsets

require (
	api v0.0.0
	github.com/alexedwards/scs/v2 v2.5.1
	github.com/google/uuid v1.6.0
	github.com/gorilla/mux v1.8.0
	go.mongodb.org/mongo-driver v1.10.3
	google.golang.org/grpc v1.63.2
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
	golang.org/x/crypto v0.19.0 // indirect
	golang.org/x/net v0.21.0 // indirect
	golang.org/x/sync v0.6.0 // indirect
	golang.org/x/sys v0.17.0 // indirect
	golang.org/x/text v0.14.0 // indirect
	google.golang.org/genproto/googleapis/rpc v0.0.0-20240227224415-6ceb2ff114de // indirect
	google.golang.org/protobuf v1.34.0 // indirect
)

replace api => ../api
