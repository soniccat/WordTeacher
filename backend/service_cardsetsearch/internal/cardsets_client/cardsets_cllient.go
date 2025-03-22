package cardsets_client

import (
	"context"
	"time"
	"tools"
	"tools/logger"

	cardsetsgrpc "service_cardsets/pkg/grpc/service_cardsets/api"

	"google.golang.org/grpc"
)

type CardSetResult struct {
	CardSet *cardsetsgrpc.CardSet
	Error   error
}

type Contract interface {
	GetCardSets(ctx context.Context, since time.Time) (chan CardSetResult, error)
}

type client struct {
	logger     *logger.Logger
	grpcClient cardsetsgrpc.CardSetsClient
}

func NewClient(logger *logger.Logger, grpcClient cardsetsgrpc.CardSetsClient) Contract {
	return &client{
		logger:     logger,
		grpcClient: grpcClient,
	}
}

func (c *client) GetCardSets(ctx context.Context, since time.Time) (chan CardSetResult, error) {
	grpcCardSetStream, err := c.grpcClient.GetCardSets(
		ctx,
		&cardsetsgrpc.GetCardSetsIn{SinceDate: tools.Ptr(tools.TimeToApiDate(since))},
		grpc.EmptyCallOption{},
	)

	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	cardSetChan := make(chan CardSetResult)
	go func() {
		for {
			cs, err := grpcCardSetStream.Recv()
			if err != nil {
				cardSetChan <- CardSetResult{Error: logger.WrapError(ctx, err)}
				close(cardSetChan)
				break
			} else {
				cardSetChan <- CardSetResult{CardSet: cs}
			}
		}
	}()

	return cardSetChan, nil
}
