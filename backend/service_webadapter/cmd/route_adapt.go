package main

import (
	"log"
	"net/http"

	"golang.org/x/net/html"

	scraper "github.com/cardigann/go-cloudflare-scraper"
)

func (app *application) adapt(w http.ResponseWriter, r *http.Request) {

	t, err := scraper.NewTransport(http.DefaultTransport)
	if err != nil {
		log.Fatal(err)
	}

	client := &http.Client{Transport: t}

	res, err := client.Get("https://www.vocabulary.com/lists/search?query=A+Little+Hatred")
	if err != nil {
		return
	}

	if res.StatusCode != 200 {
		return
	}

	// resBody, err := io.ReadAll(res.Body)
	// if err != nil {
	// 	return
	// }

	// bodyString := string(resBody)
	node, err := html.Parse(res.Body)
	if err != nil {
		return
	}

	app.logger.Info.Print("" + node.Data)

	_, validateSessionErr := app.sessionValidator.Validate(r)
	if validateSessionErr != nil {
		app.SetError(w, validateSessionErr.InnerError, validateSessionErr.StatusCode)
		return
	}

	// Path params
	// params := mux.Vars(r)

	//app.WriteResponse(w, response)
}
