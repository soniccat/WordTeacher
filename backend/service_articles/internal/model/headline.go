package model

import (
	articlesgrpc "service_articles/pkg/grpc/service_articles/api"
	"time"
	"tools"
)

type Headline struct {
	Id             string     `json:"id,omitempty" bson:"_id,omitempty"`
	SourceId       string     `json:"sourceId,omitempty" bson:"sourceId,omitempty"`
	SourceName     string     `json:"sourceName,omitempty" bson:"sourceName,omitempty"`
	SourceCategory string     `json:"sourceCategory,omitempty" bson:"sourceCategory,omitempty"`
	Title          string     `json:"title" bson:"title"`
	Description    string     `json:"description,omitempty" bson:"description,omitempty"`
	Link           string     `json:"link" bson:"link"`
	PubDate        *time.Time `json:"pubDate,omitempty" bson:"pubDate,omitempty"`
	UpdateDate     *time.Time `json:"updateDate,omitempty" bson:"updateDate,omitempty"`
	Creator        *string    `json:"creator,omitempty" bson:"creator,omitempty"`
}

func (h *Headline) LateDate() *time.Time {
	if h.UpdateDate != nil && h.PubDate != nil {
		if h.UpdateDate.Compare(*h.PubDate) == -1 {
			return h.PubDate
		}

		return h.UpdateDate
	}

	if h.UpdateDate != nil {
		return h.UpdateDate
	}

	if h.PubDate != nil {
		return h.PubDate
	}

	return nil
}

func (h *Headline) ToGrpc() *articlesgrpc.Headline {
	return &articlesgrpc.Headline{
		Id:          h.Id,
		SourceId:    h.SourceId,
		SourceName:  h.SourceName,
		Title:       h.Title,
		Description: h.Description,
		Link:        h.Link,
		PubDate:     tools.OptTimeToOptApiDate(h.PubDate),
		UpdateDate:  tools.OptTimeToOptApiDate(h.UpdateDate),
		Creator:     h.Creator,
	}
}
