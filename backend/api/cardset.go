package api

import (
	"encoding/json"
	"tools"
)

type CardSet struct {
	Id               string   `json:"id,omitempty"`
	Name             string   `json:"name"`
	Description      string   `json:"description"`
	Tags             []string `json:"tags"`
	Cards            []*Card  `json:"cards"`
	UserId           string   `json:"userId"` // TODO: consider several owners via a permission filed
	CreationDate     string   `json:"creationDate"`
	ModificationDate string   `json:"modificationDate"`
	CreationId       string   `json:"creationId"`
}

func (cs *CardSet) IsEqual(a *CardSet) bool {
	if cs.Id != a.Id {
		return false
	}
	if cs.Name != a.Name {
		return false
	}
	if cs.Description != a.Description {
		return false
	}
	if !tools.CompareSlices(cs.Tags, a.Tags) {
		return false
	}
	if cs.UserId != a.UserId {
		return false
	}
	if cs.CreationDate != a.CreationDate {
		return false
	}
	if cs.ModificationDate != a.ModificationDate {
		return false
	}
	if cs.CreationId != a.CreationId {
		return false
	}
	if len(cs.Cards) != len(a.Cards) {
		return false
	}
	for i, c := range cs.Cards {
		if !c.IsEqual(a.Cards[i]) {
			return false
		}
	}

	return true
}

func (cs *CardSet) WithoutIDs() *CardSet {
	return &CardSet{
		Id:          "",
		Name:        cs.Name,
		Description: cs.Description,
		Tags:        cs.Tags,
		Cards: tools.Map(cs.Cards, func(c *Card) *Card {
			return c.WithoutIds()
		}),
		UserId:           "",
		CreationDate:     cs.CreationDate,
		ModificationDate: cs.ModificationDate,
		CreationId:       cs.CreationId,
	}
}

func (cs *CardSet) ToJson() ([]byte, error) {
	return json.Marshal(cs)
}

type CardSetSortByName []*CardSet

func (a CardSetSortByName) Len() int           { return len(a) }
func (a CardSetSortByName) Swap(i, j int)      { a[i], a[j] = a[j], a[i] }
func (a CardSetSortByName) Less(i, j int) bool { return a[i].Name < a[j].Name }
