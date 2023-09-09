package grpc

import (
	"time"
	"tools"

	"models/session_validator"
	"service_cardsets/internal/storage"
	grpcapi "service_cardsets/pkg/grpc/service_cardsets/api"
)

type CardSetsGRPCHandler struct {
	sessionValidator  session_validator.SessionValidator
	cardSetRepository *storage.Repository
}

func NewCardSetsGRPCHandler(
	sessionValidator session_validator.SessionValidator,
	cardSetRepository *storage.Repository,
) *CardSetsGRPCHandler {
	return &CardSetsGRPCHandler{
		sessionValidator:  sessionValidator,
		cardSetRepository: cardSetRepository,
	}
}

func (s *CardSetsGRPCHandler) GetCardSets(in *grpcapi.GetCardSetsIn, server grpcapi.CardSets_GetCardSetsServer) error {
	var sinceDate *time.Time
	if in.SinceDate != nil {
		d, err := tools.ParseApiDate(*in.SinceDate)
		if err != nil {
			return err
		}

		sinceDate = &d
	}

	cardSets, err := s.cardSetRepository.ModifiedCardSetsSince(server.Context(), nil, sinceDate)
	if err != nil {
		return err
	}

	for i := range cardSets {
		err = server.Send(cardSets[i].ToGrpc())
		if err != nil {
			return err
		}
	}

	return nil
}
