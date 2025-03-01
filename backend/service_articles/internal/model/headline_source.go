package model

import (
	"time"
)

const (
	HeadlineSourceTypeRss = "rss"
)

type HeadlineSource struct {
	Id            string    `json:"id,omitempty" bson:"_id,omitempty"`
	Type          string    `json:"type" bson:"type"`
	Link          string    `json:"link" bson:"link"`
	LastCrawlDate time.Time `json:"lastCrawlDate,omitempty" bson:"lastCrawlDate,omitempty"`
	NextCrawlDate time.Time `json:"nextCrawlDate,omitempty" bson:"nextCrawlDate,omitempty"`
	CrawlPeriod   int64     `json:"crawlPeriod" bson:"crawlPeriod"`
}
