package model

import (
	"time"
)

type Headline struct {
	Id          string     `json:"id,omitempty" bson:"_id,omitempty"`
	SourceId    string     `json:"sourceId,omitempty" bson:"sourceId,omitempty"`
	Title       string     `json:"title" bson:"title"`
	Description string     `json:"description,omitempty" bson:"description,omitempty"`
	Link        string     `json:"link" bson:"link"`
	PubDate     *time.Time `json:"pubDate,omitempty" bson:"pubDate,omitempty"`
	UpdateDate  *time.Time `json:"updateDate,omitempty" bson:"updateDate,omitempty"`
	Creator     *string    `json:"creator,omitempty" bson:"creator,omitempty"`
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
