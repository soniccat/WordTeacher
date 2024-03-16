package cardsetpull_worker

import (
	"context"
	"errors"
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

	ticker := time.NewTicker(24 * time.Hour)
	go func() {
		for ; true; <-ticker.C { // run immediately and every tick
			ch, outErr := w.cardsetsClient.GetCardSets(ctx, *lastModificationDate)
			if outErr != nil {
				w.logger.Error.Print(outErr)
				continue
			}

			for r := range ch {
				if r.Error != nil {
					break
				} else if r.CardSet != nil {
					dbCardSet, outErr := model.GRPCCardSetToDb(r.CardSet)
					if outErr != nil {
						w.logger.Error.Printf("CardSetPullWorker GRPCCardSetToDb cardset %s %s", r.CardSet.Id, outErr.Error())
						continue
					}
					w.repository.UpsertCardSet(ctx, dbCardSet)
				}
			}

			if outErr != nil {
				*lastModificationDate = time.Now()
			}
		}
	}()

	return nil
}
