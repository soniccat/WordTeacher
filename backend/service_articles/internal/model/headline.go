package model

import (
	"time"
)

type Headline struct {
	Id          string     `json:"id,omitempty" bson:"_id,omitempty"`
	Title       string     `json:"title" bson:"title"`
	Description string     `json:"description,omitempty" bson:"description,omitempty"`
	Link        string     `json:"link" bson:"link"`
	PubDate     *time.Time `json:"pubDate,omitempty" bson:"pubDate,omitempty"`
	UpdateDate  *time.Time `json:"updateDate,omitempty" bson:"updateDate,omitempty"`
	Creator     *string    `json:"creator,omitempty" bson:"creator,omitempty"`
}
