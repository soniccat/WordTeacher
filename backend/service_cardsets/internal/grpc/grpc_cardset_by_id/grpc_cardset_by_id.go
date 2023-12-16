package grpc_cardset_by_id

import (
	"api"
	"context"
	"tools"

	"models/session_validator"
	"service_cardsets/internal/model"
	grpcapi "service_cardsets/pkg/grpc/service_cardsets/api"
)

type storage interface {
	LoadCardSetDbById(ctx context.Context, id string) (*model.DbCardSet, error)
}

type Handler struct {
	sessionValidator session_validator.SessionValidator
	storage          storage
}

func NewHandler(
	sessionValidator session_validator.SessionValidator,
	storage storage,
) *Handler {
	return &Handler{
		sessionValidator: sessionValidator,
		storage:          storage,
	}
}

func (s *Handler) GetCardSetById(ctx context.Context, in *grpcapi.GetCardSetIn) (*grpcapi.GetCardSetOut, error) {
	dbCardSet, err := s.storage.LoadCardSetDbById(ctx, in.Id)
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
