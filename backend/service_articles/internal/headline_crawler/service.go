package headline_crawler

import (
	"context"
	"fmt"
	"io"
	"net/http"
	"service_articles/internal/model"
	"slices"
	"sort"
	"sync"
	"time"
	"tools"
	"tools/logger"

	"github.com/microcosm-cc/bluemonday"
)

const maxHeadlinePerSource = 1000

type headlineStorage interface {
	InsertHeadlines(
		ctx context.Context,
		headlines []model.Headline,
	) error

	KeepRecentHeadlines(
		ctx context.Context,
		sourceId string,
		count int64,
	) error

	FindLatestHeadline(
		ctx context.Context,
		sourceId string,
	) (*model.Headline, error)
}

type headlineSourceStorage interface {
	FindSourcesReadyToCrawl(
		ctx context.Context,
		currentTime time.Time,
	) ([]model.HeadlineSource, error)

	FindNextCrawlDate(
		ctx context.Context,
	) (*time.Time, error)

	UpdateCrawlDate(
		ctx context.Context,
		id string,
		newNextCrawDate time.Time,
	) error
}

type Crawler struct {
	logger                *logger.Logger
	timeProvider          tools.TimeProvider
	headlineStorage       headlineStorage
	headlineSourceStorage headlineSourceStorage
	httpClient            http.Client
	ugcPolicy             *bluemonday.Policy
}

func New(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	headlineStorage headlineStorage,
	headlineSourceStorage headlineSourceStorage,
) *Crawler {
	httpClient := http.Client{}

	policy := bluemonday.StrictPolicy()
	policy.AllowAttrs("href").OnElements("a")

	return &Crawler{
		logger:                logger,
		timeProvider:          timeProvider,
		headlineStorage:       headlineStorage,
		headlineSourceStorage: headlineSourceStorage,
		httpClient:            httpClient,
		ugcPolicy:             policy,
	}
}

func (c *Crawler) Start(ctx context.Context) {
	for {
		sources, err := c.headlineSourceStorage.FindSourcesReadyToCrawl(ctx, c.timeProvider.Now())
		if err != nil {
			c.logger.ErrorWithError(ctx, err, "findReadySources")
		}

		group := sync.WaitGroup{}
		for i := range sources {
			group.Add(1)
			go func(s model.HeadlineSource) {
				defer group.Done()
				e := c.crawlSource(ctx, s)
				if e != nil {
					c.logger.ErrorWithError(ctx, e, "crawlSource")
				}
			}(sources[i])
		}
		group.Wait()

		nextCrawlDate, err := c.headlineSourceStorage.FindNextCrawlDate(ctx)
		if err != nil || nextCrawlDate == nil {
			if err != nil {
				c.logger.ErrorWithError(ctx, err, "findNextCrawlDate")
			}
			time.Sleep(time.Duration(10) * time.Minute)
			continue
		}

		if nextCrawlDate != nil {
			d := nextCrawlDate.Sub(c.timeProvider.Now())
			time.Sleep(d)
		}
	}
}

func (c *Crawler) crawlSource(ctx context.Context, source model.HeadlineSource) error {
	lastHeadline, err := c.headlineStorage.FindLatestHeadline(ctx, source.Id)
	if err != nil {
		return err
	}

	parser := resolveParser(source.Type, c.ugcPolicy)
	if parser == nil {
		return fmt.Errorf("can't find parser for type %s", source.Type)
	}

	r, err := http.NewRequest("GET", source.Link, nil)
	if err != nil {
		return logger.WrapError(ctx, err)
	}
	requestResponse, err := c.httpClient.Do(r)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	if requestResponse.StatusCode != http.StatusOK {
		return fmt.Errorf("error response code: %d", requestResponse.StatusCode)
	}

	headlines, err := parser.Parse(ctx, requestResponse.Body)
	if err != nil {
		return logger.WrapError(ctx, err)
	}

	newHeadlines := headlines
	if lastHeadline != nil {
		newHeadlines = tools.Filter(headlines, func(h model.Headline) bool {
			if lastHeadline.Date.Compare(h.Date) != 1 && lastHeadline.Link != h.Link {
				return true
			}

			return false
		})
	}

	// deduplicate
	sort.Sort(model.HeadlineSortByLink(newHeadlines))
	newHeadlines = slices.CompactFunc(newHeadlines, func(h1 model.Headline, h2 model.Headline) bool {
		return h1.Link == h2.Link
	})

	for i := range newHeadlines {
		newHeadlines[i].SourceId = source.Id
		newHeadlines[i].SourceName = source.Title
		newHeadlines[i].SourceCategory = source.Category
	}

	if len(newHeadlines) > 0 {
		err = c.headlineStorage.InsertHeadlines(ctx, newHeadlines)
		if err != nil {
			return err
		}

		c.logger.Info(ctx, fmt.Sprintf("Source %s: added %d headlines", source.Title, len(newHeadlines)))
	}

	nextCrawlDate := c.timeProvider.Now().Add(time.Duration(source.CrawlPeriod))
	err = c.headlineSourceStorage.UpdateCrawlDate(ctx, source.Id, nextCrawlDate)
	if err != nil {
		return err
	}

	if len(newHeadlines) > 0 {
		err = c.headlineStorage.KeepRecentHeadlines(ctx, source.Id, maxHeadlinePerSource)
		if err != nil {
			return err
		}
	}

	return nil
}

type sourceParser interface {
	Parse(ctx context.Context, r io.Reader) ([]model.Headline, error)
}

func resolveParser(t string, ugcPolicy *bluemonday.Policy) sourceParser {
	if t == model.HeadlineSourceTypeRss {
		return &RssParser{
			ugcPolicy,
		}
	} else if t == model.HeadlineSourceTypeAtom {
		return &AtomParser{
			ugcPolicy,
		}
	}

	return nil
}
