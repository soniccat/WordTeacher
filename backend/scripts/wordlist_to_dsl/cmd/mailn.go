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
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

type WordEntry struct {
	// Order          int           //`bson:"order"`
	Term           string        `bson:"term,omitempty"`
	TermLowercased string        `bson:"term_lowercased,omitempty"`
	Transcriptions []string      `bson:"transcriptions,omitempty"`
	Audios         []WordAudio   `bson:"audios,omitempty"`
	Etymology      int           `bson:"etymology,omitempty"`
	DefPairs       []WordDefPair `bson:"defs,omitempty"`
}

type WordAudio struct {
	FileName      string  `bson:"fileName"`
	Accent        *string `bson:"accent,omitempty"`
	Transcription *string `bson:"transcription,omitempty"`
	Text          *string `bson:"text,omitempty"`
}

type WordDefPair struct {
	PartOfSpeech string         `bson:"partofspeech,omitempty"`
	DefEntries   []WordDefEntry `bson:"defentries,omitempty"`
}

type WordDefEntry struct {
	Def      WordDef  `bson:"def,omitempty"`
	Examples []string `bson:"examples,omitempty"`
	Synonyms []string `bson:"synonyms,omitempty"`
	Antonyms []string `bson:"antonyms,omitempty"`
}

type WordDef struct {
	Value  string   `bson:"value,omitempty"`
	Labels []string `bson:"labels,omitempty"`
}

type WordExamples struct {
	Examples []WordExample `bson:"examples,omitempty"`
	Word     WordEntry     `bson:"word"`
}

type WordExample struct {
	Example       string  `bson:"example"`
	DefPairIndex  int     `bson:"defPairIndex"`
	DefEntryIndex int     `bson:"defEntryIndex"`
	ExampleIndex  int     `bson:"exampleIndex"`
	TextScore     float64 `bson:"textScore"`
}

///

func main() {
	os.Exit(run())
}

type MongoTermWrapper struct {
	Term string `bson:"term"`
}

type MongoTermWrapperList []MongoTermWrapper

func run() int {
	mongoURI := flag.String("mongoURI", "mongodb://localhost:27017/?directConnection=true&replicaSet=rs0", "Database hostname url")
	inputFilePath := flag.String("input", "./default.wordlist", "Input wordlist file path")
	outputFilePath := flag.String("output", "./output.dsl", "Output wordlist file path")
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

	// read input file
	f, e := os.Open(exPath + *inputFilePath)
	if e != nil {
		log.Fatalln(e)
	}
	defer f.Close()

	outF.WriteString("#NAME	\"Wiktionary EN-EN base\"\n")
	outF.WriteString("#INDEX_LANGUAGE	\"English\"\n")
	outF.WriteString("#CONTENTS_LANGUAGE	\"English\"\n")
	outF.WriteString("\n")

	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		t := scanner.Text()

		entries, _ := findWordEntries(ctx, t, collection)
		if len(entries) > 0 {
			writeDslEntry(entries, outF)
		}
	}

	return 1
}

func writeDslEntry(wordEntries []WordEntry, f *os.File) {
	currentTerm := ""
	for _, e := range wordEntries {
		if currentTerm != e.Term {
			f.WriteString(e.Term + "\n")
		}

		if len(e.Transcriptions) > 0 {
			f.WriteString("\t")
			for _, t := range e.Transcriptions {
				f.WriteString("[t]" + t + "[/t]")
			}
			f.WriteString("\n")
		}

		if len(e.Audios) > 0 {
			f.WriteString("\t")
			for _, a := range e.Audios {
				f.WriteString("[s]" + "https://aglushkov.ru/" + a.FileName + "[/s]")

				resultText := a.Accent
				if resultText == nil {
					resultText = a.Text
				}

				if a.Transcription != nil {
					f.WriteString("[t]" + *a.Transcription + "[/t]")
				}

				if resultText != nil {
					f.WriteString(*resultText)
				}
			}
			f.WriteString("\n")
		}

		for _, dp := range e.DefPairs {
			f.WriteString("\t[p]" + dp.PartOfSpeech + "[/p]\n")
			for _, de := range dp.DefEntries {
				f.WriteString("\t")
				f.WriteString("[trn]")
				for _, l := range de.Def.Labels {
					f.WriteString("[p]" + l + "[/p]")
				}
				f.WriteString(strings.Replace(de.Def.Value, "\n", "\\n", -1))
				f.WriteString("[/trn]\n")

				for _, e := range de.Examples {
					f.WriteString("\t")
					f.WriteString("[ex]" + strings.Replace(e, "\n", "\\n", -1) + "[/ex]")
					f.WriteString("\n")
				}

				if len(de.Synonyms) > 0 {
					f.WriteString("\tSyn:\n")
					for _, s := range de.Synonyms {
						f.WriteString("\t" + s + "\n")
					}
				}

				if len(de.Antonyms) > 0 {
					f.WriteString("\tAnt:\n")
					for _, a := range de.Antonyms {
						f.WriteString("\t" + a + "\n")
					}
				}
			}
		}
	}
}

func findWordEntries(
	ctx context.Context,
	term string,
	collection *mongo.Collection,
) ([]WordEntry, error) {
	cursor, err := collection.Find(
		ctx,
		bson.M{"term_lowercased": term},
		options.Find().SetSort(
			bson.M{
				"etymology": 1,
			},
		),
	)
	if err != nil {
		return nil, err
	}

	var result []WordEntry
	err = cursor.All(ctx, &result)
	if err != nil {
		return nil, err
	}

	return result, nil
}
