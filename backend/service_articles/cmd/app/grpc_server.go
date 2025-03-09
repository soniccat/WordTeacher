package main

import (
	"tools/logger"

	"service_articles/internal/grpc/get_headlines"
	grpcapi "service_articles/pkg/grpc/service_articles/api"
)

type HeadlinesServer struct {
	grpcapi.UnimplementedHeadlinesServer
	logger              *logger.Logger
	getHeadlinesHandler get_headlines.Handler
}

func NewHeadlinesServer(app *application) *HeadlinesServer {
	s := &HeadlinesServer{
		logger:              app.logger,
		getHeadlinesHandler: *get_headlines.NewHandler(app.logger, app.headlineStorage),
	}
	return s
}

func (s *HeadlinesServer) GetHeadlines(in *grpcapi.GetHeadlinesIn, server grpcapi.Headlines_GetHeadlinesServer) error {
	err := s.getHeadlinesHandler.GetHeadlines(in, server)
	if err != nil {
		s.logger.ErrorWithError(server.Context(), err, "GetHeadlines error")
	}

	return err
}
