package partofspeech

type PartOfSpeech = uint8

const (
	Undefined = PartOfSpeech(iota)
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
