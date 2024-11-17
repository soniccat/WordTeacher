package main

import (
	"bufio"
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
	TermLowercased string `bson:"term_lowercased"`
}

type MongoTermWrapperList []MongoTermWrapper

func run() int {
	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	inputFilePath := flag.String("input", "./default.wordlist", "Input wordlist file path")
	outputFilePath := flag.String("output", "./default_filtered.wordlist", "Output wordlist file path")
	flag.Parse()

	ex, err := os.Executable()
	if err != nil {
		panic(err)
	}
	exPath := filepath.Dir(ex) + "/"

	f, e := os.Open(exPath + *inputFilePath)
	if e != nil {
		log.Fatalln(e)
	}
	defer f.Close()

	outF, e := os.OpenFile(exPath+*outputFilePath, os.O_APPEND|os.O_WRONLY|os.O_CREATE, 0600)
	if e != nil {
		log.Fatalln(e)
	}
	defer outF.Close()

	outF2, e := os.OpenFile(exPath+*outputFilePath+"_not_found", os.O_APPEND|os.O_WRONLY|os.O_CREATE, 0600)
	if e != nil {
		log.Fatalln(e)
	}
	defer outF2.Close()

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
		Projection: bson.M{"term_lowercased": 1},
	})
	if e != nil {
		log.Fatalln(e)
	}
	var mongoResult MongoTermWrapperList
	e = c.All(ctx, &mongoResult)
	if e != nil {
		log.Fatalln(e)
	}

	dbTermSet := make(map[string]bool)
	for _, r := range mongoResult {
		if _, ok := dbTermSet[r.TermLowercased]; !ok {
			dbTermSet[r.TermLowercased] = true
		}
	}

	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		t := scanner.Text()
		if _, ok := dbTermSet[strings.ToLower(t)]; ok {
			outF.WriteString(t + "\n")
		} else {
			outF2.WriteString(t + "\n")
		}
	}

	return 1
}
