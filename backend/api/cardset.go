package api

import (
	"tools"
)

type CardSet struct {
	Id                  string      `json:"id,omitempty"`
	Name                string      `json:"name"`
	Tags                []string    `json:"tags"`
	Cards               []*Card     `json:"cards"`
	Terms               []string    `json:"terms,omitempty"`
	UserId              string      `json:"userId"` // TODO: consider several owners via a permission filed
	CreationDate        string      `json:"creationDate"`
	ModificationDate    string      `json:"modificationDate"`
	CreationId          string      `json:"creationId"`
	Info                CardSetInfo `json:"info"`
	IsAvailableInSearch bool        `json:"isAvailableInSearch"`
}

type CardSetInfo struct {
	Description string  `json:"description"`
	Source      *string `json:"source"` // url
}

func (cs *CardSet) WithoutIDs() *CardSet {
	return &CardSet{
		Id:   "",
		Name: cs.Name,
		Tags: cs.Tags,
		Cards: tools.Map(cs.Cards, func(c *Card) *Card {
			return c.WithoutIds()
		}),
		Terms:               cs.Terms,
		UserId:              "",
		CreationDate:        cs.CreationDate,
		ModificationDate:    cs.ModificationDate,
		CreationId:          cs.CreationId,
		Info:                cs.Info,
		IsAvailableInSearch: cs.IsAvailableInSearch,
	}
}

type CardSetSortByName []*CardSet

func (a CardSetSortByName) Len() int           { return len(a) }
func (a CardSetSortByName) Swap(i, j int)      { a[i], a[j] = a[j], a[i] }
func (a CardSetSortByName) Less(i, j int) bool { return a[i].Name < a[j].Name }
