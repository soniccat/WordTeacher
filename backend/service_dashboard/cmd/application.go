package main

import (
	"context"
	"service_articles/internal/model"
	"time"
	"tools"

	"github.com/alexedwards/scs/v2"

	"models/session_validator"
	"tools/logger"
)

var sources []model.HeadlineSource = []model.HeadlineSource{
	{
		IntId:       1,
		Title:       "The Guardian",
		Description: "Latest news, sport, business, comment, analysis and reviews from the Guardian, the world's leading liberal voice",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://www.theguardian.com/uk/rss",
		CrawlPeriod: int64(time.Duration(30) * time.Minute),
		Category:    model.HeadlineSourceCategoryNews,
	},
	{
		IntId:       2,
		Title:       "NBC News",
		Description: "NBC News Top Stories",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://feeds.nbcnews.com/nbcnews/public/news",
		CrawlPeriod: int64(time.Duration(30) * time.Minute),
		Category:    model.HeadlineSourceCategoryNews,
	},
	{
		IntId:       3,
		Title:       "Hacker News",
		Description: "Links for the intellectually curious, ranked by readers.",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://news.ycombinator.com/rss",
		CrawlPeriod: int64(time.Duration(30) * time.Minute),
		Category:    model.HeadlineSourceCategoryTech,
	},
}

type application struct {
	logger                *logger.Logger
	timeProvider          tools.TimeProvider
	sessionManager        *scs.SessionManager
	headlineStorage       *headlines.Storage
	headlineSourceStorage *headline_sources.Storage
	sessionValidator      session_validator.SessionValidator
}

func createApplication(
	ctx context.Context,
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionManager *scs.SessionManager,
	sessionValidator session_validator.SessionValidator,
	headlineStorage *headlines.Storage,
	headlineSourceStorage *headline_sources.Storage,
) (_ *application, err error) {
	app := &application{
		logger:                logger,
		timeProvider:          timeProvider,
		sessionManager:        sessionManager,
		headlineStorage:       headlineStorage,
		headlineSourceStorage: headlineSourceStorage,
		sessionValidator:      sessionValidator,
	}

	defer func() {
		if err != nil {
			app.stop()
		}
	}()

	err = app.updateSourcesInStorage(ctx)
	if err != nil {
		return nil, err
	}

	return app, nil
}

func (app *application) stop() {
	app.headlineStorage.StopMongo()
}

func (app *application) updateSourcesInStorage(ctx context.Context) error {
	currentSources, err := app.headlineSourceStorage.AllSources(ctx)
	if err != nil {
		return err
	}

	newSources := tools.Filter(sources, func(currentSource model.HeadlineSource) bool {
		foundSource := tools.FindOrNil(currentSources, func(newSource model.HeadlineSource) bool {
			return newSource.IntId == currentSource.IntId
		})

		return foundSource == nil
	})

	err = app.headlineSourceStorage.InsertHeadlineSources(ctx, newSources)
	if err != nil {
		return err
	}

	return nil
}

func (app *application) createCrawler() *headline_crawler.Crawler {
	return headline_crawler.New(
		app.logger,
		app.timeProvider,
		app.headlineStorage,
		app.headlineSourceStorage,
	)
}
