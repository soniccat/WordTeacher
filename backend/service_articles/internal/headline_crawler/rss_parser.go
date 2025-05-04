package headline_crawler

import (
	"context"
	"html"
	"io"
	"time"
	"tools"
	"tools/logger"

	"github.com/microcosm-cc/bluemonday"
	"github.com/nbio/xml"

	"service_articles/internal/model"
)

type rss struct {
	Channels []channel `xml:"channel"`
}

type channel struct {
	Title       string    `xml:"title"`
	Description string    `xml:"description"`
	Link        string    `xml:"link"`
	Updated     time.Time `xml:"dc:date"`
	Items       []item    `xml:"item"`
}

type item struct {
	Title       string     `xml:"title"`
	Description string     `xml:"description"`
	Link        string     `xml:"link"`
	PubDate     *rssTime   `xml:"pubDate"`
	UpdateDate  *rssTime   `xml:"updateDate"`
	Date        *time.Time `xml:"dc:date"`
	Author      *string    `xml:"dc:creator"`
}

func (i *item) GetLateDate() *time.Time {
	return tools.GetLatestDate(i.Date, i.UpdateDate.GetTime(), i.PubDate.GetTime())
}

type rssTime struct {
	time.Time
}

func (c *rssTime) GetTime() *time.Time {
	if c == nil {
		return nil
	}

	return &c.Time
}

func (c *rssTime) UnmarshalXML(d *xml.Decoder, start xml.StartElement) error {
	var v string
	d.DecodeElement(&v, &start)
	parse, err := time.Parse(time.RFC1123, v)
	if err != nil {
		parse, err = time.Parse(time.RFC1123Z, v)
	}

	if err != nil {
		return err
	}
	*c = rssTime{parse}
	return nil
}

type RssParser struct {
	ugcPolicy *bluemonday.Policy
}

func NewRssParser(ugcPolicy *bluemonday.Policy) RssParser {
	return RssParser{
		ugcPolicy: ugcPolicy,
	}
}

func (p *RssParser) Parse(ctx context.Context, r io.Reader) ([]model.Headline, error) {
	rss := rss{}

	decoder := xml.NewDecoder(r)
	err := decoder.Decode(&rss)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	var headlines []model.Headline
	for i := range rss.Channels {
		for ci := range rss.Channels[i].Items {
			item := rss.Channels[i].Items[ci]

			lateDate := item.GetLateDate()
			if lateDate == nil {
				continue
			}

			headlines = append(headlines, model.Headline{
				Title:       html.UnescapeString(p.ugcPolicy.Sanitize(item.Title)),
				Description: html.UnescapeString(p.ugcPolicy.Sanitize(item.Description)),
				Link:        item.Link,
				Date:        *lateDate,
				Creator:     item.Author,
			})
		}
	}

	return headlines, nil
}
