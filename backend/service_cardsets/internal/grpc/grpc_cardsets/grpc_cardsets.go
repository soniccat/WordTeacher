package grpc_cardsets

import (
	"context"
	"time"
	"tools"

	"models/session_validator"
	"service_cardsets/internal/model"
	grpcapi "service_cardsets/pkg/grpc/service_cardsets/api"
)

type storage interface {
	ModifiedCardSetsSince(ctx context.Context, lastModificationDate *time.Time) ([]*model.DbCardSet, error)
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

func (s *Handler) GetCardSets(in *grpcapi.GetCardSetsIn, server grpcapi.CardSets_GetCardSetsServer) error {
	var sinceDate *time.Time
	if in.SinceDate != nil {
		d, err := tools.ParseApiDate(*in.SinceDate)
		if err != nil {
			return err
		}

		sinceDate = &d
	}

	cardSets, err := s.storage.ModifiedCardSetsSince(server.Context(), sinceDate)
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
