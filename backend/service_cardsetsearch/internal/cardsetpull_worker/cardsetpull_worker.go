package cardsetpull_worker

import (
	"context"
	"errors"
	"fmt"
	"time"

	"service_cardsetsearch/internal/cardsets_client"
	"service_cardsetsearch/internal/model"
	"service_cardsetsearch/internal/storage"
	"tools/logger"

	"go.mongodb.org/mongo-driver/mongo"
)

type CardSetPullWorker struct {
	logger         *logger.Logger
	cardsetsClient cardsets_client.Contract
	repository     *storage.Repository
}

func NewCardSetPullWorker(
	logger *logger.Logger,
	cardsetsClient cardsets_client.Contract,
	repository *storage.Repository,
) *CardSetPullWorker {
	return &CardSetPullWorker{
		logger:         logger,
		cardsetsClient: cardsetsClient,
		repository:     repository,
	}
}

func (w *CardSetPullWorker) Start(ctx context.Context) error {
	lastModificationDate, err := w.repository.LastCardSetModificationDate(ctx)

	if err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			lastModificationDate = &time.Time{}
		} else {
			return err
		}
	}

	ticker := time.NewTicker(30 * time.Minute)
	go func() {
		for ; true; <-ticker.C { // run immediately and every tick
			ch, outErr := w.cardsetsClient.GetCardSets(ctx, *lastModificationDate)
			if outErr != nil {
				w.logger.ErrorWithError(ctx, outErr, "CardSetPullWorker GetCardSets")
				continue
			}

			for r := range ch {
				if r.Error != nil {
					continue
				} else if r.CardSet != nil && r.CardSet.IsAvailableInSearch {
					dbCardSet, outErr := model.GRPCCardSetToDb(ctx, r.CardSet)
					if outErr != nil {
						w.logger.ErrorWithError(ctx, outErr, fmt.Sprintf("CardSetPullWorker GRPCCardSetToDb cardset %s %v", r.CardSet.Id, outErr))
						continue
					}
					outErr = w.repository.UpsertCardSet(ctx, dbCardSet)
					if outErr != nil {
						w.logger.ErrorWithError(ctx, outErr, fmt.Sprintf("CardSetPullWorker UpsertCardSet cardset %s %v", dbCardSet.Id, outErr))
					}
				}
			}

			*lastModificationDate = time.Now()
		}
	}()

	return nil
}
