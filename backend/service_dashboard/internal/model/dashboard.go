package model

import (
	"api"
	"context"
	"slices"
	"time"
	"tools"

	cardsetsgrpc "service_cardsets/pkg/grpc/service_cardsets/api"
)

type DashboardHeadlineBlock struct {
	Categories []DashboardHeadlineCategory `json:"categories,omitempty" bson:"categories,omitempty"`
}

type DashboardHeadlineCategory struct {
	CategoryName string              `json:"categoryName,omitempty" bson:"categoryName,omitempty"`
	Headlines    []DashboardHeadline `json:"headlines,omitempty" bson:"headlines,omitempty"`
}

type DashboardHeadline struct {
	Id             string    `json:"id,omitempty" bson:"_id,omitempty"`
	SourceName     string    `json:"sourceName,omitempty" bson:"sourceName,omitempty"`
	SourceCategory string    `json:"sourceCategory,omitempty" bson:"sourceCategory,omitempty"`
	Title          string    `json:"title" bson:"title"`
	Description    string    `json:"description,omitempty" bson:"description,omitempty"`
	Link           string    `json:"link" bson:"link"`
	Date           time.Time `json:"date,omitempty" bson:"date,omitempty"`
	Creator        *string   `json:"creator,omitempty" bson:"creator,omitempty"`
}

type DashboardNewCardsSetBlock struct {
	CardSets []api.CardSet `json:"cardsets,omitempty" bson:"cardsets,omitempty"`
}

func GRPCCardSetToApi(ctx context.Context, cs *cardsetsgrpc.CardSet) api.CardSet {
	terms := tools.Map(cs.Cards, func(c *cardsetsgrpc.Card) string {
		return c.Term
	})
	slices.Sort(terms) // to work in the same way it works in search

	cardSetDb := api.CardSet{
		Id:               cs.Id,
		Name:             cs.Name,
		Tags:             cs.Tags,
		UserId:           cs.UserId,
		CreationDate:     cs.CreationDate,
		ModificationDate: cs.ModificationDate,
		Terms:            terms,
		Info: api.CardSetInfo{
			Description: cs.Info.Description,
			Source:      cs.Info.Source,
		},
	}
	return cardSetDb
}
