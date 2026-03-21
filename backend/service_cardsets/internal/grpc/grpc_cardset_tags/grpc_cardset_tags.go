package grpc_cardset_tags

import (
	"context"

	"models/session_validator"
	"service_cardsets/internal/model"
	grpcapi "service_cardsets/pkg/grpc/service_cardsets/api"
)

type storage interface {
	CountTags(ctx context.Context) ([]model.Tag, error)
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

func (s *Handler) GetCardSetTags(ctx context.Context, in *grpcapi.GetCardSetTagsIn) (*grpcapi.GetCardSetTagsOut, error) {
	tagCount, err := s.storage.CountTags(ctx)
	if err != nil {
		return nil, err
	}

	totalCount := int64(0)
	tags := []*grpcapi.Tag{}
	for _, tag := range tagCount {
		tags = append(tags, &grpcapi.Tag{
			Name:  tag.Name,
			Count: tag.Count,
		})
		totalCount += tag.Count
	}

	out := grpcapi.GetCardSetTagsOut{
		Tags: tags,
	}
	return &out, nil
}
