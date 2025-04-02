package grpc_cardsets

import (
	"context"
	"time"
	"tools"
	"tools/logger"

	"models/session_validator"
	"service_cardsets/internal/model"
	grpcapi "service_cardsets/pkg/grpc/service_cardsets/api"
)

type storage interface {
	ModifiedCardSetsSince(ctx context.Context, lastModificationDate *time.Time, onlyAvailableInSearch bool, limit int64) ([]*model.DbCardSet, error)
}

type Handler struct {
	logger           *logger.Logger
	sessionValidator session_validator.SessionValidator
	storage          storage
}

func NewHandler(
	logger *logger.Logger,
	sessionValidator session_validator.SessionValidator,
	storage storage,
) *Handler {
	return &Handler{
		logger:           logger,
		sessionValidator: sessionValidator,
		storage:          storage,
	}
}

func (s *Handler) GetCardSets(in *grpcapi.GetCardSetsIn, server grpcapi.CardSets_GetCardSetsServer) error {
	s.logger.Info(server.Context(), "GetCardSets starts")
	var sinceDate *time.Time
	if in.SinceDate != nil {
		d, err := tools.ParseApiDate(server.Context(), *in.SinceDate)
		if err != nil {
			return logger.WrapError(server.Context(), err)
		}

		sinceDate = &d
	}

	cardSets, err := s.storage.ModifiedCardSetsSince(
		server.Context(),
		sinceDate,
		tools.PtrBoolValue(in.OnlyAvailableInSearch),
		tools.PtrInt64Value(in.Limit),
	)
	if err != nil {
		return logger.WrapError(server.Context(), err)
	}

	s.logger.Info(server.Context(), "GetCardSets got cardSets", "count", len(cardSets))
	for i := range cardSets {
		err = server.Send(cardSets[i].ToGrpc())
		if err != nil {
			return logger.WrapError(server.Context(), err)
		}
	}
	s.logger.Info(server.Context(), "GetCardSets finishes")

	return nil
}
