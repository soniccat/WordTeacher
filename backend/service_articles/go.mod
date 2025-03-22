module service_articles

go 1.22.2

require tools v0.0.0

replace tools => ../tools

require models v0.0.0

replace models => ../models

require (
	github.com/alexedwards/scs/v2 v2.5.1
	github.com/google/uuid v1.6.0
	github.com/gorilla/mux v1.8.1
	github.com/microcosm-cc/bluemonday v1.0.27
	github.com/nbio/xml v0.0.0-20250127210239-7f9281fed8c6
	github.com/stretchr/testify v1.8.0
	go.mongodb.org/mongo-driver v1.17.2
	google.golang.org/grpc v1.71.0
	google.golang.org/protobuf v1.36.5
)

require (
	github.com/alexedwards/scs/redisstore v0.0.0-20230305153148-62e546ce9d2d // indirect
	github.com/aymerick/douceur v0.2.0 // indirect
	github.com/davecgh/go-spew v1.1.1 // indirect
	github.com/golang/snappy v0.0.4 // indirect
	github.com/gomodule/redigo v1.8.9 // indirect
	github.com/gorilla/css v1.0.1 // indirect
	github.com/klauspost/compress v1.16.7 // indirect
	github.com/montanaflynn/stats v0.7.1 // indirect
	github.com/pmezard/go-difflib v1.0.0 // indirect
	github.com/xdg-go/pbkdf2 v1.0.0 // indirect
	github.com/xdg-go/scram v1.1.2 // indirect
	github.com/xdg-go/stringprep v1.0.4 // indirect
	github.com/youmark/pkcs8 v0.0.0-20240726163527-a2c0da244d78 // indirect
	golang.org/x/crypto v0.32.0 // indirect
	golang.org/x/net v0.34.0 // indirect
	golang.org/x/sync v0.10.0 // indirect
	golang.org/x/sys v0.29.0 // indirect
	golang.org/x/text v0.21.0 // indirect
	google.golang.org/genproto/googleapis/rpc v0.0.0-20250115164207-1a7da9e5054f // indirect
	gopkg.in/yaml.v3 v3.0.1 // indirect
)
