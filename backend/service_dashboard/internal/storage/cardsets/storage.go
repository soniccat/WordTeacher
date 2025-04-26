package cardsets

import (
	"api"
	"context"
	"errors"
	"fmt"
	"io"
	"runtime/debug"
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
	) (chan cardsets.CardSetResult, error)
}

type Storage struct {
	logger   *logger.Logger
	client   client
	cardSets []api.CardSet
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

			var newCardSets []api.CardSet
			cardSets, err := s.client.GetCardSets(ctx, 15)
			if err != nil {
				s.logger.ErrorWithError(ctx, err, "cardsets.StartPulling")
				return
			}

			s.logger.Info(ctx, "Starts pulling cardsets")
			for c := range cardSets {
				if c.Error != nil {
					if !errors.Is(c.Error, io.EOF) {
						s.logger.ErrorWithError(ctx, c.Error, "StartPulling.broken cardSet")
					}
					continue
				}

				newCardSets = append(newCardSets, model.GRPCCardSetToApi(ctx, c.CardSet))
			}
			s.logger.Info(ctx, "Ends pulling cardsets")
			s.cardSets = newCardSets
		})
	}()
}
