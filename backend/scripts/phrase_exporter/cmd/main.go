package main

import (
	"context"
	"flag"
	"log"
	"os"
	"path/filepath"
	"strings"

	"tools/mongowrapper"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo/options"
)

func main() {
	os.Exit(run())
}

type MongoTermWrapper struct {
	Term string `bson:"term"`
}

type MongoTermWrapperList []MongoTermWrapper

func run() int {
	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	outputFilePath := flag.String("output", "./default_filtered.wordlist", "Output wordlist file path")
	flag.Parse()

	ex, err := os.Executable()
	if err != nil {
		panic(err)
	}
	exPath := filepath.Dir(ex) + "/"

	outF, e := os.OpenFile(exPath+*outputFilePath, os.O_APPEND|os.O_WRONLY|os.O_CREATE, 0600)
	if e != nil {
		log.Fatalln(e)
	}
	defer outF.Close()

	mw, e := mongowrapper.New(*mongoURI, false)
	if e != nil {
		log.Fatalln(e)
	}
	defer mw.Stop()

	e = mw.Connect()
	if e != nil {
		log.Fatalln(e)
	}

	ctx := context.Background()
	collection := mw.Client.Database("wiktionary").Collection("words2")
	c, e := collection.Find(ctx, bson.M{}, &options.FindOptions{
		Projection: bson.M{"term": 1},
	})
	if e != nil {
		log.Fatalln(e)
	}
	var mongoResult MongoTermWrapperList
	e = c.All(ctx, &mongoResult)
	if e != nil {
		log.Fatalln(e)
	}

	for _, r := range mongoResult {
		if strings.Contains(r.Term, " ") {
			outF.WriteString(r.Term + "\n")
		}
	}

	return 1
}
