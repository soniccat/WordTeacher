package cardsetpull_worker

import (
	"context"
	"time"

	"service_cardsetsearch/internal/cardsets_client"
	"service_cardsetsearch/internal/model"
	"service_cardsetsearch/internal/storage"
	"tools/logger"
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
		return err
	}

	ticker := time.NewTicker(24 * time.Hour)
	go func() {
		for {
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
					if err != nil {
						w.logger.Error.Printf("CardSetPullWorker GRPCCardSetToDb cardset %s %s", r.CardSet.Id, outErr.Error())
						continue
					}
					w.repository.UpsertCardSet(ctx, dbCardSet)
				}
			}

			if outErr != nil {
				*lastModificationDate = time.Now()
			}

			<-ticker.C
		}
	}()

	return nil
}