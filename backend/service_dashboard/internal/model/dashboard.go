package model

import "time"

// type Dashboard struct {
// 	HeadlineBlock DashboardHeadlineBlock `json:"headlineBlock"`
// }

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
