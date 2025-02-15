module service_articles

go 1.22.2

require tools v0.0.0

replace tools => ../tools

require api v0.0.0

replace api => ../api

require (
	go.mongodb.org/mongo-driver v1.17.2
	models v0.0.0
)

replace models => ../models
