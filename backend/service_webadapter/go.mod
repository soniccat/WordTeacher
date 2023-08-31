module service_webadapter

go 1.19

require tools v0.0.1

replace tools => ../tools

require models v0.0.1

replace models => ../models

require (
	github.com/cardigann/go-cloudflare-scraper v0.0.0-20200425223932-91bd9b1006f2
	github.com/google/uuid v1.3.0
	github.com/gorilla/mux v1.8.0
)

require (
	github.com/robertkrimen/otto v0.2.1 // indirect
	golang.org/x/text v0.4.0 // indirect
	gopkg.in/sourcemap.v1 v1.0.5 // indirect
)
