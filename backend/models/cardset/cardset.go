package cardset

import (
	"models/card"
)

type CardSet struct {
	Name  string      `json:"name"`
	Date  *string     `json:"date"`
	Cards []card.Card `json:"cards"`
}
