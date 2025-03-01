module service_articles

go 1.22.2

require tools v0.0.0

replace tools => ../tools

require models v0.0.0

replace models => ../models

require (
	github.com/nbio/xml v0.0.0-20250127210239-7f9281fed8c6
	go.mongodb.org/mongo-driver v1.17.2
	models v0.0.0
)
