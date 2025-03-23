package get_headlines

import (
	"context"
	"time"
	"tools"
	"tools/logger"

	"service_articles/internal/model"
	grpcapi "service_articles/pkg/grpc/service_articles/api"
)

type storage interface {
	FindHeadlines(
		ctx context.Context,
		category string,
		limit int64,
		since *time.Time,
	) ([]model.Headline, error)
}

type Handler struct {
	logger  *logger.Logger
	storage storage
}

func NewHandler(
	logger *logger.Logger,
	storage storage,
) *Handler {
	return &Handler{
		logger:  logger,
		storage: storage,
	}
}

func (s *Handler) GetHeadlines(in *grpcapi.GetHeadlinesIn, server grpcapi.Headlines_GetHeadlinesServer) error {
	var since *time.Time
	if in.Since != nil {
		parsedSince, err := tools.ParseApiDate(server.Context(), *in.Since)
		if err != nil {
			return logger.WrapError(
				server.Context(),
				tools.NewInvalidArgumentError("since", in.Since, "wrong format", err),
			)
		}

		since = &parsedSince
	}

	headlines, err := s.storage.FindHeadlines(
		server.Context(),
		categoryNameById(int32(in.Category)),
		tools.PtrInt64Value(in.Limit),
		since,
	)
	if err != nil {
		return logger.WrapError(server.Context(), err)
	}

	s.logger.Info(server.Context(), "GetHeadlines got headlines", "count", len(headlines))
	for i := range headlines {
		err = server.Send(headlines[i].ToGrpc())
		if err != nil {
			return logger.WrapError(server.Context(), err)
		}
	}

	return nil
}

func categoryNameById(category int32) string {
	switch category {
	case int32(grpcapi.Category_NEWS):
		return model.HeadlineSourceCategoryNews
	case int32(grpcapi.Category_TECH):
		return model.HeadlineSourceCategoryTech
	}

	return model.HeadlineSourceCategoryAll
}
