package rabbitmq

import (
	"api"
	"encoding/json"
	"fmt"
	"tools"
)

const (
	TypeUpdate = iota
	TypeDelete
)

type Message struct {
	Type    int             `json:"type"`
	Content json.RawMessage `json:"content"`
}

type CardSet struct {
	Id               string   `json:"id,omitempty"`
	Name             string   `json:"name"`
	Description      string   `json:"description"`
	Tags             []string `json:"tags"`
	Cards            []*Card  `json:"cards"`
	UserId           string   `json:"userId"`
	CreationDate     string   `json:"creationDate"`
	ModificationDate string   `json:"modificationDate"`
}

type Card struct {
	Id               string           `json:"id"`
	Term             string           `json:"term"`
	Transcription    *string          `json:"transcription,omitempty"`
	PartOfSpeech     api.PartOfSpeech `json:"partOfSpeech"`
	Definitions      []string         `json:"definitions"`
	Synonyms         []string         `json:"synonyms"`
	Examples         []string         `json:"examples"`
	UserId           string           `json:"userId"`
	CreationDate     string           `json:"creationDate"`
	ModificationDate string           `json:"modificationDate"`
}

func NewWithUpdate(cardSet *CardSet) (*Message, error) {
	bytes, err := json.Marshal(cardSet)
	if err != nil {
		return nil, err
	}

	return &Message{
		Type:    TypeUpdate,
		Content: bytes,
	}, nil
}

func NewWithDelete(cardSetId string) (*Message, error) {
	bytes, err := json.Marshal(cardSetId)
	if err != nil {
		return nil, err
	}

	return &Message{
		Type:    TypeDelete,
		Content: bytes,
	}, nil
}

func (m *Message) GetCardSet() (*CardSet, error) {
	if m.Type != TypeUpdate {
		return nil, fmt.Errorf("GetCardSet is called for Type = %d", m.Type)
	}

	var cardSet CardSet
	err := json.Unmarshal(m.Content, &cardSet)
	return &cardSet, err
}

func (m *Message) GetDeletedCardSetId() (*string, error) {
	if m.Type != TypeDelete {
		return nil, fmt.Errorf("GetDeletedCardSetId is called for Type = %d", m.Type)
	}

	var cardSetId string
	err := json.Unmarshal(m.Content, &cardSetId)
	return &cardSetId, err
}

func CardSetFromApiCardSet(cs *api.CardSet) *CardSet {
	cards := tools.Map(cs.Cards, func(c *api.Card) *Card {
		return CardFromApiCard(c)
	})

	cardSetDb := &CardSet{
		Id:               cs.Id,
		Name:             cs.Name,
		Cards:            cards,
		UserId:           cs.UserId,
		CreationDate:     cs.CreationDate,
		ModificationDate: cs.ModificationDate,
	}
	return cardSetDb
}

func CardFromApiCard(c *api.Card) *Card {
	return &Card{
		Id:               c.Id,
		Term:             c.Term,
		Transcription:    c.Transcription,
		PartOfSpeech:     c.PartOfSpeech,
		Definitions:      c.Definitions,
		Synonyms:         c.Synonyms,
		Examples:         c.Examples,
		UserId:           c.UserId,
		CreationDate:     c.CreationDate,
		ModificationDate: c.ModificationDate,
	}
}
