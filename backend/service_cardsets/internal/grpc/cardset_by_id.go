package grpc

import (
	"api"
	"context"
	"tools"

	"go.mongodb.org/mongo-driver/bson/primitive"

	"models/session_validator"
	"service_cardsets/internal/model"
	"service_cardsets/internal/storage"
	grpcapi "service_cardsets/pkg/grpc/service_cardsets/api"
)

type CardSetByIdGRPCHandler struct {
	sessionValidator  session_validator.SessionValidator
	cardSetRepository *storage.Repository
}

func NewCardSetByIdGRPCHandler(
	sessionValidator session_validator.SessionValidator,
	cardSetRepository *storage.Repository,
) *CardSetByIdGRPCHandler {
	return &CardSetByIdGRPCHandler{
		sessionValidator:  sessionValidator,
		cardSetRepository: cardSetRepository,
	}
}

func (s *CardSetByIdGRPCHandler) GetCardSetById(ctx context.Context, in *grpcapi.GetCardSetIn) (*grpcapi.GetCardSetOut, error) {
	cardSetDbId, err := primitive.ObjectIDFromHex(in.Id)
	if err != nil {
		return nil, err
	}

	dbCardSet, err := s.cardSetRepository.LoadCardSetDbByObjectID(ctx, cardSetDbId)
	if err != nil {
		return nil, err
	}

	// cut progress data
	defaultCardProgress := &api.CardProgress{
		CurrentLevel:     0,
		LastMistakeCount: 0,
		LastLessonDate:   "",
	}
	dbCardSet.Cards = tools.Map(dbCardSet.Cards, func(c *model.DbCard) *model.DbCard {
		c.Progress = defaultCardProgress
		return c
	})

	out := grpcapi.GetCardSetOut{}
	out.CardSet = dbCardSet.ToGrpc()

	return &out, nil
}
