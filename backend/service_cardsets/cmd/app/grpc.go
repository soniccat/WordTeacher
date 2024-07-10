package main

import (
	"context"
	"tools/logger"

	"service_cardsets/internal/grpc/grpc_cardset_by_id"
	"service_cardsets/internal/grpc/grpc_cardsets"
	grpcapi "service_cardsets/pkg/grpc/service_cardsets/api"
)

type CardSetsServer struct {
	grpcapi.UnimplementedCardSetsServer
	logger             *logger.Logger
	cardsetByIdHandler grpc_cardset_by_id.Handler
	cardsetsHandler    grpc_cardsets.Handler
}

func NewCardSetServer(app *application) *CardSetsServer {
	s := &CardSetsServer{
		logger:             app.logger,
		cardsetByIdHandler: *grpc_cardset_by_id.NewHandler(app.sessionValidator, app.cardSetRepository),
		cardsetsHandler:    *grpc_cardsets.NewHandler(app.logger, app.sessionValidator, app.cardSetRepository),
	}
	return s
}

func (s *CardSetsServer) GetCardSets(in *grpcapi.GetCardSetsIn, server grpcapi.CardSets_GetCardSetsServer) error {
	err := s.cardsetsHandler.GetCardSets(in, server)
	if err != nil {
		s.logger.ErrorWithError(server.Context(), err, "GetCardSets error")
	}

	return err
}

func (s *CardSetsServer) GetCardSetById(ctx context.Context, in *grpcapi.GetCardSetIn) (*grpcapi.GetCardSetOut, error) {
	out, err := s.cardsetByIdHandler.GetCardSetById(ctx, in)
	if err != nil {
		s.logger.ErrorWithError(ctx, err, "GetCardSetById error")
	}

	return out, err
}
