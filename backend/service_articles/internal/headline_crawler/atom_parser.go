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

type atom struct {
	Entries []entry `xml:"entry"`
}

type entry struct {
	Title       string     `xml:"title"`
	Description string     `xml:"summary"`
	Link        link       `xml:"link"`
	PubDate     *time.Time `xml:"published"`
	UpdateDate  *time.Time `xml:"updated"`
	Author      *author    `xml:"author"`
}

type link struct {
	Href string `xml:"href,attr"`
}

type author struct {
	Name string `xml:"name"`
}

func (e *entry) GetLateDate() *time.Time {
	return tools.GetLatestDate(e.UpdateDate, e.PubDate)
}

func (e *entry) AuthorName() *string {
	if e.Author == nil {
		return nil
	}

	return &e.Author.Name
}

type atomTime struct {
	time.Time
}

func (c *atomTime) GetTime() *time.Time {
	if c == nil {
		return nil
	}

	return &c.Time
}

func (c *atomTime) UnmarshalXML(d *xml.Decoder, start xml.StartElement) error {
	var v string
	d.DecodeElement(&v, &start)
	parse, err := time.Parse(time.RFC1123, v)
	if err != nil {
		return err
	}
	*c = atomTime{parse}
	return nil
}

type AtomParser struct {
	ugcPolicy *bluemonday.Policy
}

func NewAtomParser(ugcPolicy *bluemonday.Policy) AtomParser {
	return AtomParser{
		ugcPolicy: ugcPolicy,
	}
}

func (p *AtomParser) Parse(ctx context.Context, r io.Reader) ([]model.Headline, error) {
	rss := atom{}

	decoder := xml.NewDecoder(r)
	err := decoder.Decode(&rss)
	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	var headlines []model.Headline
	for i := range rss.Entries {
		entry := rss.Entries[i]

		lateDate := entry.GetLateDate()
		if lateDate == nil {
			continue
		}

		headlines = append(headlines, model.Headline{
			Title:       html.UnescapeString(p.ugcPolicy.Sanitize(entry.Title)),
			Description: html.UnescapeString(p.ugcPolicy.Sanitize(entry.Description)),
			Link:        entry.Link.Href,
			Date:        *lateDate,
			Creator:     entry.AuthorName(),
		})
	}

	return headlines, nil
}
