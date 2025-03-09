package get_headlines

import (
	"context"
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
	headlines, err := s.storage.FindHeadlines(
		server.Context(),
		in.Category.String(),
		tools.PtrInt64Value(in.Limit),
	)
	if err != nil {
		return logger.WrapError(server.Context(), err)
	}

	for i := range headlines {
		err = server.Send(headlines[i].ToGrpc())
		if err != nil {
			return logger.WrapError(server.Context(), err)
		}
	}

	return nil
}
