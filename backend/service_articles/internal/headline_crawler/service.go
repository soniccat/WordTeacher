package headline_crawler

import (
	"context"
	"service_articles/internal/model"
	"sync"
	"time"
	"tools"
	"tools/logger"
)

const (
	crawl_success = 1
	crawl_failed  = 2
)

type headlineStorage interface {
	InsertHeadlines(
		ctx context.Context,
		headlines []model.Headline,
	) error
}

type headlineSourceStorage interface {
	FindSourcesReadyToCrawl(
		ctx context.Context,
		currentTime time.Time,
	) ([]model.HeadlineSource, error)

	FindNextCrawlDate(
		ctx context.Context,
	) (*time.Time, error)
}

type Crawler struct {
	logger                *logger.Logger
	timeProvider          tools.TimeProvider
	headlineStorage       headlineStorage
	headlineSourceStorage headlineSourceStorage
}

func New(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	headlineStorage headlineStorage,
	headlineSourceStorage headlineSourceStorage,
) *Crawler {
	return &Crawler{
		logger:                logger,
		timeProvider:          timeProvider,
		headlineStorage:       headlineStorage,
		headlineSourceStorage: headlineSourceStorage,
	}
}

func (c *Crawler) Start(ctx context.Context) {
	//var crawlResultChan chan int
	for {
		sources, err := c.headlineSourceStorage.FindSourcesReadyToCrawl(ctx, c.timeProvider.Now())
		if err != nil {
			c.logger.ErrorWithError(ctx, err, "findReadySources")
			time.Sleep(time.Duration(10) * time.Minute)
			continue
		}

		group := sync.WaitGroup{}
		for i, _ := range sources {
			group.Add(1)
			go func(s model.HeadlineSource) {
				defer group.Done()
				e := c.crawlSource(s)
				if e != nil {
					c.logger.ErrorWithError(ctx, e, "crawlSource")
				}
			}(sources[i])
		}
		group.Wait()

		nextCrawlDate, err := c.headlineSourceStorage.FindNextCrawlDate(ctx)
		if nextCrawlDate != nil {
			d := nextCrawlDate.Sub(time.Now().UTC())
			time.Sleep(d)
		}
	}
}

func (c *Crawler) crawlSource(source model.HeadlineSource) error {
	return nil
}
