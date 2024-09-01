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
	case "noun", "proper noun":
		return Noun
	case "verb":
		return Verb
	case "adjective", "adj":
		return Adjective
	case "adverb", "adv":
		return Adverb
	case "pronoun", "pron":
		return Pronoun
	case "preposition", "prep":
		return Preposition
	case "conjunction", "con":
		return Conjunction
	case "interjection", "interj":
		return Interjection
	case "abbreviation":
		return Abbreviation
	case "exclamation":
		return Exclamation
	case "determiner", "det":
		return Determiner
	case "phrasalverb":
		return PhrasalVerb
	}

	// TODO: figure out what is "en-num", "en-part", "en-postp",

	return Undefined
}
