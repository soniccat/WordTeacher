package main

import (
	"context"

	"service_cardsets/internal/grpc/grpc_cardset_by_id"
	"service_cardsets/internal/grpc/grpc_cardsets"
	grpcapi "service_cardsets/pkg/grpc/service_cardsets/api"
)

type CardSetsServer struct {
	grpcapi.UnimplementedCardSetsServer
	cardsetByIdHandler grpc_cardset_by_id.Handler
	cardsetsHandler    grpc_cardsets.Handler
}

func NewCardSetServer(app *application) *CardSetsServer {
	s := &CardSetsServer{
		cardsetByIdHandler: *grpc_cardset_by_id.NewHandler(app.sessionValidator, app.cardSetRepository),
		cardsetsHandler:    *grpc_cardsets.NewHandler(app.sessionValidator, app.cardSetRepository),
	}
	return s
}

func (s *CardSetsServer) GetCardSets(in *grpcapi.GetCardSetsIn, server grpcapi.CardSets_GetCardSetsServer) error {
	return s.cardsetsHandler.GetCardSets(in, server)
}

func (s *CardSetsServer) GetCardSetById(ctx context.Context, in *grpcapi.GetCardSetIn) (*grpcapi.GetCardSetOut, error) {
	return s.cardsetByIdHandler.GetCardSetById(ctx, in)
}
