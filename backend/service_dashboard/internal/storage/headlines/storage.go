package headlines

import (
	"context"
	"time"
	"tools"
	"tools/logger"

	"service_dashboard/internal/client/articles"
	"service_dashboard/internal/model"

	articlesgrpc "service_articles/pkg/grpc/service_articles/api"
)

var categoriesToPull = []int32{
	int32(articlesgrpc.Category_ALL),
	int32(articlesgrpc.Category_NEWS),
	int32(articlesgrpc.Category_TECH),
}
var pullLimit = 30

type client interface {
	GetHeadlines(
		ctx context.Context,
		category int32,
		limit int64,
	) (chan articles.HeadlinesResult, error)
}

type Storage struct {
	logger     *logger.Logger
	client     client
	categories []model.DashboardHeadlineCategory
}

func New(
	logger *logger.Logger,
	client client,
) Storage {
	return Storage{
		logger: logger,
		client: client,
	}
}

func (s *Storage) HeadlineCategories() []model.DashboardHeadlineCategory {
	return s.categories
}

func (s *Storage) StartPulling(ctx context.Context) {
	go func() {
		tools.StartTicker(ctx, 30*time.Minute, func() {
			for _, c := range categoriesToPull {
				s.pullCategory(ctx, c)
			}
		})
	}()
}

func (s *Storage) pullCategory(ctx context.Context, category int32) (model.DashboardHeadlineCategory, error) {
	headlines, err := s.client.GetHeadlines(ctx, category, int64(pullLimit))
	if err != nil {
		s.logger.ErrorWithError(ctx, err, "pullCategory.GetHeadlines")
		return model.DashboardHeadlineCategory{}, err
	}

	categoryName := categoryNameById(category)
	var dashboardHeadlines []model.DashboardHeadline
	for r := range headlines {
		h := r.Headline
		if r.Error == nil && h != nil {
			date, err := tools.ParseApiDate(ctx, h.Date)
			s.logger.ErrorWithError(ctx, err, "pullCategory.ParseApiDate")

			dashboardHeadlines = append(dashboardHeadlines, model.DashboardHeadline{
				Id:             h.Id,
				SourceName:     h.SourceName,
				SourceCategory: categoryName,
				Title:          h.Title,
				Description:    h.Description,
				Link:           h.Link,
				Date:           date,
				Creator:        h.Creator,
			})
		}
	}

	return model.DashboardHeadlineCategory{
		CategoryName: categoryName,
		Headlines:    dashboardHeadlines,
	}, nil
}

func categoryNameById(category int32) string {
	switch category {
	case int32(articlesgrpc.Category_NEWS):
		return "News"
	case int32(articlesgrpc.Category_TECH):
		return "Tech"
	}

	return "All"
}
