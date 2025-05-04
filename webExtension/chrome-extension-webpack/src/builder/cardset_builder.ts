import {v4 as uuidv4} from 'uuid';
import { JSONBuilder } from "./jsonbuilder"

export enum PartOfSpeech {
  Undefined = 0,
  Noun = 1,
  Verb = 2,
  Adjective = 3,
  Adverb = 4,
  Pronoun = 5,
  Preposition = 6,
  Conjunction = 7,
  Interjection = 8,
  Abbreviation = 9,
  Exclamation = 10,
  Determiner = 11,
  PhrasalVerb = 12
}

export class CardSetBuilder {
  jsonBuilder: JSONBuilder = new JSONBuilder()
  creationDate: string

  cardSetObj: any = null
  currentCard: any = null
  cardDefinitions: Array<string> = []
  cardExamples: Array<string> = []
  cardSynonyms: Array<string> = []

  constructor(name: string, source: string) {
    this.creationDate = new Date().toISOString()

    this.cardSetObj = this.jsonBuilder.cursor
    this.setName(name)
    this.setInfoSource(source)
    this.jsonBuilder.set("id", "")
    this.jsonBuilder.set("creationId", uuidv4())
    this.jsonBuilder.set("creationDate", this.creationDate)
    this.jsonBuilder.set("modificationDate", this.creationDate)
    this.jsonBuilder.startArray("cards")
  }

  setName(value: string) {
    this.cardSetObj["name"] = value
  }

  setInfoDescription(value: string) {
    this.obtainInfo()["description"] = value
  }

  setInfoSource(value: string) {
    this.obtainInfo()["source"] = value
  }

  obtainInfo(): any {
    let infoObj = this.cardSetObj["info"]
    if (infoObj == null) {
      infoObj = {}
      this.cardSetObj["info"] = infoObj
    }
    return infoObj
  }

  json(): any {
    return this.jsonBuilder.json
  }

  startCard() {
    if (this.currentCard != null) {
      this.jsonBuilder.endObject()
    }

    this.jsonBuilder.startArrayObject()
    this.currentCard = this.jsonBuilder.cursor

    this.jsonBuilder.set("id", "")
    this.jsonBuilder.set("creationId", uuidv4())
    this.jsonBuilder.set("creationDate", this.creationDate)
    this.jsonBuilder.set("modificationDate", this.creationDate)
    this.jsonBuilder.set("progress", {})
    this.jsonBuilder.set("exampleTermSpans", [])
    this.jsonBuilder.set("definitionTermSpans", [])
    this.jsonBuilder.set("needToUpdateDefinitionSpans", true)
    this.jsonBuilder.set("needToUpdateExampleSpans", true)

    this.cardDefinitions = []
    this.cardExamples = []
    this.cardSynonyms = []

    this.setPartOfSpeech(PartOfSpeech.Undefined)
    this.jsonBuilder.set("definitions", this.cardDefinitions)
    this.jsonBuilder.set("examples", this.cardExamples)
    this.jsonBuilder.set("synonyms", this.cardSynonyms)
  }

  setCardTerm(term: string) {
    this.currentCard["term"] = term
  }

  setPartOfSpeech(partOfSpeech: PartOfSpeech) {
    this.jsonBuilder.set("partOfSpeech", partOfSpeech)
  }

  addCardDefinition(definition: string) {
    this.cardDefinitions.push(definition)
  }

  addCardExample(example: string) {
    this.cardExamples.push(example)
  }

  addCardSynonym(synonym: string) {
    this.cardSynonyms.push(synonym)
  }

  endCard() {
    this.jsonBuilder.endObject()
    this.currentCard = null
  }
}
