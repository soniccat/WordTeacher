package main

import (
	"context"

	"service_cardsets/internal/grpc"
	grpcapi "service_cardsets/pkg/grpc/service_cardsets/api"
)

type CardSetsServer struct {
	grpcapi.UnimplementedCardSetsServer
	grpc.CardSetByIdGRPCHandler
	grpc.CardSetsGRPCHandler
}

func NewCardSetServer(app *application) *CardSetsServer {
	s := &CardSetsServer{
		CardSetByIdGRPCHandler: *grpc.NewCardSetByIdGRPCHandler(app.sessionValidator, app.cardSetRepository),
		CardSetsGRPCHandler:    *grpc.NewCardSetsGRPCHandler(app.sessionValidator, app.cardSetRepository),
	}
	return s
}

func (s *CardSetsServer) GetCardSets(in *grpcapi.GetCardSetsIn, server grpcapi.CardSets_GetCardSetsServer) error {
	return s.CardSetsGRPCHandler.GetCardSets(in, server)
}

func (s *CardSetsServer) GetCardSetById(ctx context.Context, in *grpcapi.GetCardSetIn) (*grpcapi.GetCardSetOut, error) {
	return s.CardSetByIdGRPCHandler.GetCardSetById(ctx, in)
}
