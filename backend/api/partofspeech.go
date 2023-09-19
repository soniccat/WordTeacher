package api

import "strings"

type PartOfSpeech = uint8

const (
	Undefined PartOfSpeech = iota
	Noun
	Verb
	Adjective
	Adverb
	Pronoun
	Preposition
	Conjunction
	Interjection
	Abbreviation
	Exclamation
	Determiner
	PhrasalVerb
)

func PartOfSpeechFromString(str string) PartOfSpeech {
	switch strings.ToLower(str) {
	case "noun":
		return Noun
	case "verb":
		return Verb
	case "adjective":
		return Adjective
	case "adverb":
		return Adverb
	case "pronoun":
		return Pronoun
	case "preposition":
		return Preposition
	case "conjunction":
		return Conjunction
	case "interjection":
		return Interjection
	case "abbreviation":
		return Abbreviation
	case "exclamation":
		return Exclamation
	case "determiner":
		return Determiner
	case "phrasalverb":
		return PhrasalVerb
	}

	return Undefined
}
