package headline_crawler

import (
	"fmt"
	"io"
	"service_articles/internal/model"
	"time"

	"github.com/nbio/xml"
)

type rss struct {
	Channels []channel `xml:"channel"`
}

type channel struct {
	Title       string    `xml:"title"`
	Description string    `xml:"description"`
	Link        string    `xml:"link"`
	Updated     time.Time `xml:"dc:date"`
	// Author      Person    `xml:"author"`
	Entry []item `xml:"item"`
}

type item struct {
	Title       string    `xml:"title"`
	Description string    `xml:"description"`
	Link        string    `xml:"link"`
	Updated     time.Time `xml:"dc:date"`
	// Author      Person    `xml:"author"`
	// Summary     Text      `xml:"summary"`
}

type RssParser struct {
}

func (p *RssParser) Parse(r io.Reader) (model.HeadlineSource, error) {
	rss := rss{}

	decoder := xml.NewDecoder(r)
	err := decoder.Decode(&rss)
	if err != nil {
		fmt.Printf("Error Decode: %v\n", err)
		return model.HeadlineSource{}, err
	}

	return model.HeadlineSource{}, nil
}
