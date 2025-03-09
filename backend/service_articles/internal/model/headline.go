package model

import (
	"time"
	"tools"

	articlesgrpc "service_articles/pkg/grpc/service_articles/api"
)

type Headline struct {
	Id             string    `json:"id,omitempty" bson:"_id,omitempty"`
	SourceId       string    `json:"sourceId,omitempty" bson:"sourceId,omitempty"`
	SourceName     string    `json:"sourceName,omitempty" bson:"sourceName,omitempty"`
	SourceCategory string    `json:"sourceCategory,omitempty" bson:"sourceCategory,omitempty"`
	Title          string    `json:"title" bson:"title"`
	Description    string    `json:"description,omitempty" bson:"description,omitempty"`
	Link           string    `json:"link" bson:"link"`
	Date           time.Time `json:"date,omitempty" bson:"date,omitempty"`
	Creator        *string   `json:"creator,omitempty" bson:"creator,omitempty"`
}

func (h *Headline) ToGrpc() *articlesgrpc.Headline {
	return &articlesgrpc.Headline{
		Id:          h.Id,
		SourceId:    h.SourceId,
		SourceName:  h.SourceName,
		Title:       h.Title,
		Description: h.Description,
		Link:        h.Link,
		Date:        tools.TimeToApiDate(h.Date),
		Creator:     h.Creator,
	}
}

// sort
type HeadlineSortByLink []Headline

func (a HeadlineSortByLink) Len() int           { return len(a) }
func (a HeadlineSortByLink) Swap(i, j int)      { a[i], a[j] = a[j], a[i] }
func (a HeadlineSortByLink) Less(i, j int) bool { return a[i].Link < a[j].Link }
