package cardsets

import (
	"api"
	"context"
	"errors"
	"fmt"
	"io"
	"runtime/debug"
	cardsetsgrpc "service_cardsets/pkg/grpc/service_cardsets/api"
	"service_dashboard/internal/client/cardsets"
	"service_dashboard/internal/model"
	"time"
	"tools"
	"tools/logger"
)

type client interface {
	GetCardSets(
		ctx context.Context,
		limit int64,
		withTag *string,
	) (chan cardsets.CardSetResult, error)

	GetCardSetTags(ctx context.Context) ([]*cardsetsgrpc.Tag, error)
}

type Storage struct {
	logger          *logger.Logger
	client          client
	cardSets        []api.CardSet
	tagWithCardSets []api.TagWithCardSets
}

func New(
	logger *logger.Logger,
	client client,
) Storage {
	return Storage{
		logger: logger,
		client: client,
	}
}

func (s *Storage) CardSets() []api.CardSet {
	return s.cardSets
}

func (s *Storage) TagWithCardSets() []api.TagWithCardSets {
	return s.tagWithCardSets
}

func (s *Storage) StartPulling(ctx context.Context) {
	go func() {
		tools.StartTicker(ctx, 30*time.Minute, func() {
			defer func() {
				msg := "panic"
				if r := recover(); r != nil {
					msg = fmt.Sprintf("panic: %v\n%s\n", r, string(debug.Stack()))
				}
				logger.Error(context.Background(), msg)
			}()

			s.logger.Info(ctx, "Starts pulling cardsets")

			// v1 - all cardsets
			var err error
			s.cardSets, err = s.PullCardSets(ctx, nil)
			if err != nil {
				s.logger.ErrorWithError(ctx, err, "cardsets.StartPulling all cardsets")
			}

			// v2 tags and card sets per tag
			tags, err := s.client.GetCardSetTags(ctx)
			if err != nil {
				s.logger.ErrorWithError(ctx, err, "cardsets.StartPulling tags")
			}

			tagWithCardSets := []api.TagWithCardSets{}
			for _, tag := range tags {
				cardSets, err := s.PullCardSets(ctx, &tag.Name)
				if err != nil {
					s.logger.ErrorWithError(ctx, err, "cardsets.StartPulling all cardsets")
					continue
				}

				tagWithCardSets = append(tagWithCardSets, api.TagWithCardSets{
					Tag: api.CardSetTag{
						Name:  tag.Name,
						Count: tag.Count,
					},
					CardSets: cardSets,
				})
			}

			s.tagWithCardSets = tagWithCardSets
			s.logger.Info(ctx, "Ends pulling cardsets")
		})
	}()
}

func (s *Storage) PullCardSets(ctx context.Context, withTag *string) ([]api.CardSet, error) {
	var newCardSets []api.CardSet
	cardSets, err := s.client.GetCardSets(ctx, 15, withTag)
	if err != nil {
		return nil, err
	}

	for c := range cardSets {
		if c.Error != nil {
			if !errors.Is(c.Error, io.EOF) {
				s.logger.ErrorWithError(ctx, c.Error, "StartPulling.broken cardSet")
			}
			continue
		}

		newCardSets = append(newCardSets, model.GRPCCardSetToApi(ctx, c.CardSet))
	}

	return newCardSets, nil
}
