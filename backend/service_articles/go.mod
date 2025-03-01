module service_articles

go 1.22.2

require tools v0.0.0

replace tools => ../tools

require models v0.0.0

replace models => ../models

require (
	go.mongodb.org/mongo-driver v1.17.2
	models v0.0.0
)

