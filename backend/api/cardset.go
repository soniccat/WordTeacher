package api

import (
	"tools"
)

type ApiCardSet struct {
	Id               string     `json:"id,omitempty"`
	Name             string     `json:"name"`
	Cards            []*ApiCard `json:"cards"`
	UserId           string     `json:"userId"` // TODO: consider several owners via a permission filed
	CreationDate     string     `json:"creationDate"`
	ModificationDate string     `json:"modificationDate"`
	CreationId       string     `json:"creationId"`
}

func (cs *ApiCardSet) IsEqual(a *ApiCardSet) bool {
	if cs.Id != a.Id {
		return false
	}
	if cs.Name != a.Name {
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

func (cs *ApiCardSet) WithoutIDs() *ApiCardSet {
	return &ApiCardSet{
		Id:   "",
		Name: cs.Name,
		Cards: tools.Map(cs.Cards, func(c *ApiCard) *ApiCard {
			return c.WithoutIds()
		}),
		UserId:           "",
		CreationDate:     cs.CreationDate,
		ModificationDate: cs.ModificationDate,
		CreationId:       cs.CreationId,
	}
}

type ApiCardSetSortByName []*ApiCardSet

func (a ApiCardSetSortByName) Len() int           { return len(a) }
func (a ApiCardSetSortByName) Swap(i, j int)      { a[i], a[j] = a[j], a[i] }
func (a ApiCardSetSortByName) Less(i, j int) bool { return a[i].Name < a[j].Name }
