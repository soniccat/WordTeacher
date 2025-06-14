package main

import (
	"context"
	"service_articles/internal/headline_crawler"
	"service_articles/internal/model"
	"service_articles/internal/storage/headline_sources"
	"service_articles/internal/storage/headlines"
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
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
		Category:    model.HeadlineSourceCategoryNews,
	},
	{
		IntId:       2,
		Title:       "NBC News",
		Description: "NBC News Top Stories",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://feeds.nbcnews.com/nbcnews/public/news",
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
		Category:    model.HeadlineSourceCategoryNews,
	},
	{
		IntId:       3,
		Title:       "Hacker News",
		Description: "Links for the intellectually curious, ranked by readers.",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://news.ycombinator.com/rss",
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
		Category:    model.HeadlineSourceCategoryTech,
	},
	{
		IntId:       4,
		Title:       "TechCrunch",
		Description: "Startup and Technology News",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://techcrunch.com/feed",
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
		Category:    model.HeadlineSourceCategoryTech,
	},
	{
		IntId:       5,
		Title:       "The Verge",
		Description: "The Verge is about technology and how it makes us feel. Founded in 2011, we offer our audience everything from breaking news to reviews to award-winning features and investigations, on our site, in video, and in podcasts.",
		Type:        model.HeadlineSourceTypeAtom,
		Link:        "https://www.theverge.com/rss/index.xml",
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
		Category:    model.HeadlineSourceCategoryTech,
	},
	{
		IntId:       6,
		Title:       "Engadget",
		Description: "Engadget is a web magazine with obsessive daily coverage of everything new in gadgets and consumer electronics",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://www.engadget.com/rss.xml",
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
		Category:    model.HeadlineSourceCategoryTech,
	},
	{
		IntId:       7,
		Title:       "Wired",
		Description: "The latest from www.wired.com",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://www.wired.com/feed/rss",
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
		Category:    model.HeadlineSourceCategoryTech,
	},
	{
		IntId:       9,
		Title:       "Scientific American",
		Description: "Scientific American is the essential guide to the most awe-inspiring advances in science and technology, explaining how they change our understanding of the world and shape our lives.",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://www.scientificamerican.com/platform/syndication/rss/",
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
		Category:    model.HeadlineSourceCategoryScience,
	},
	{
		IntId:       10,
		Title:       "Science News",
		Description: "INDEPENDENT JOURNALISM SINCE 1921",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://www.sciencenews.org/feed",
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
		Category:    model.HeadlineSourceCategoryScience,
	},
	{
		IntId:       11,
		Title:       "ScienceBlogs",
		Description: "ScienceBlogs - Where the world discusses science",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://scienceblogs.com/rss.xml",
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
		Category:    model.HeadlineSourceCategoryScience,
	},
	{
		IntId:       12,
		Title:       "CNN",
		Description: "CNN.com delivers up-to-the-minute news and information on the latest top stories, weather, entertainment, politics and more.",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "http://rss.cnn.com/rss/edition_world.rss",
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
		Category:    model.HeadlineSourceCategoryNews,
	},
	{
		IntId:       13,
		Title:       "ABC News: International",
		Description: "ABC News: International",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://abcnews.go.com/abcnews/internationalheadlines",
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
		Category:    model.HeadlineSourceCategoryNews,
	},
	{
		IntId:       14,
		Title:       "World - CBSNews.com",
		Description: "World From CBSNews.com",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://www.cbsnews.com/latest/rss/world",
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
		Category:    model.HeadlineSourceCategoryNews,
	},
	{
		IntId:       15,
		Title:       "CoRecursive: Coding Stories",
		Description: "The stories and people behind the code.  Hear stories of software development from interesting people.",
		Type:        model.HeadlineSourceTypeRss,
		Link:        "https://corecursive.com/feed",
		CrawlPeriod: int64(time.Duration(60) * time.Minute),
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
