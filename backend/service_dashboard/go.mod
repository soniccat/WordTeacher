module service_dashboard

go 1.22.2

require tools v0.0.0

replace tools => ../tools

require models v0.0.0

replace models => ../models

require service_cardsets v0.0.0

replace service_cardsets => ../service_cardsets

require (
	github.com/alexedwards/scs/v2 v2.5.1
	github.com/gorilla/mux v1.8.1
	google.golang.org/grpc v1.71.0
	service_articles v0.0.0
)

require (
	github.com/alexedwards/scs/redisstore v0.0.0-20230305153148-62e546ce9d2d // indirect
	github.com/gomodule/redigo v1.8.9 // indirect
	go.mongodb.org/mongo-driver v1.17.2 // indirect
	golang.org/x/net v0.34.0 // indirect
	golang.org/x/sys v0.29.0 // indirect
	golang.org/x/text v0.21.0 // indirect
	google.golang.org/genproto/googleapis/rpc v0.0.0-20250115164207-1a7da9e5054f // indirect
	google.golang.org/protobuf v1.36.5 // indirect
)

replace service_articles => ../service_articles
