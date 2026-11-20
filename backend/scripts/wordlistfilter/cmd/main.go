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
	filterWordlistPath := flag.String("filterWordlist", "", "Filter wordlist file path (if set, filter by this wordlist instead of mongo)")
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

	var dbTermSet map[string]bool
	if *filterWordlistPath != "" {
		dbTermSet, e = loadTermSetFromWordlist(exPath + *filterWordlistPath)
	} else {
		dbTermSet, e = loadTermSetFromMongo(*mongoURI)
	}
	if e != nil {
		log.Fatalln(e)
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

func loadTermSetFromWordlist(path string) (map[string]bool, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer f.Close()

	termSet := make(map[string]bool)
	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		t := strings.ToLower(scanner.Text())
		if _, ok := termSet[t]; !ok {
			termSet[t] = true
		}
	}
	if err := scanner.Err(); err != nil {
		return nil, err
	}

	return termSet, nil
}

func loadTermSetFromMongo(mongoURI string) (map[string]bool, error) {
	mw, err := mongowrapper.New(mongoURI, false)
	if err != nil {
		return nil, err
	}
	defer mw.Stop()

	err = mw.Connect()
	if err != nil {
		return nil, err
	}

	ctx := context.Background()
	collection := mw.Client.Database("wiktionary").Collection("words2")
	c, err := collection.Find(ctx, bson.M{}, &options.FindOptions{
		Projection: bson.M{"term_lowercased": 1},
	})
	if err != nil {
		return nil, err
	}
	var mongoResult MongoTermWrapperList
	err = c.All(ctx, &mongoResult)
	if err != nil {
		return nil, err
	}

	termSet := make(map[string]bool)
	for _, r := range mongoResult {
		if _, ok := termSet[r.TermLowercased]; !ok {
			if !strings.Contains(r.TermLowercased, " ") {
				termSet[r.TermLowercased] = true
			}
		}
	}

	return termSet, nil
}
