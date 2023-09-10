package cardsetpull_worker

import (
	"context"
	"service_cardsetsearch/internal/cardsets_client"
	"service_cardsetsearch/internal/storage"
)

type CardSetPullWorker struct {
	cardsetsClient cardsets_client.Contract
	repository     *storage.Repository
}

func NewCardSetPullWorker(cardsetsClient cardsets_client.Contract) *CardSetPullWorker {
	return &CardSetPullWorker{
		cardsetsClient: cardsetsClient,
	}
}

func (w *CardSetPullWorker) Start(ctx context.Context) error {
	lastModificationDate, err := w.repository.LastCardSetModificationDate(ctx)

	if err != nil {
		return err
	}

	ch, err := w.cardsetsClient.GetCardSets(ctx, *lastModificationDate)
	if err != nil {
		return err
	}

	// TODO: read channel and insert

	return nil
}
