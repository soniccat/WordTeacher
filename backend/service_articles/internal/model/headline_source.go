package model

import (
	"time"
)

const (
	HeadlineSourceTypeRss  = "rss"
	HeadlineSourceTypeAtom = "atom"
)

const (
	HeadlineSourceCategoryAll     = "all"
	HeadlineSourceCategoryTech    = "tech"
	HeadlineSourceCategoryNews    = "news"
	HeadlineSourceCategoryScience = "science"
)

type HeadlineSource struct {
	Id            string    `json:"id,omitempty" bson:"_id,omitempty"`
	IntId         int64     `json:"intId,omitempty" bson:"intId,omitempty"`
	Title         string    `json:"title" bson:"title"`
	Description   string    `json:"description" bson:"description"`
	Type          string    `json:"type" bson:"type"`
	Link          string    `json:"link" bson:"link"`
	NextCrawlDate time.Time `json:"nextCrawlDate,omitempty" bson:"nextCrawlDate,omitempty"`
	CrawlPeriod   int64     `json:"crawlPeriod" bson:"crawlPeriod"`
	Category      string    `json:"category" bson:"category"`
}
