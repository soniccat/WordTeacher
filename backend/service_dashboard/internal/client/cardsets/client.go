package cardsets

import (
	"context"
	"tools"
	"tools/logger"

	cardsetsgrpc "service_cardsets/pkg/grpc/service_cardsets/api"

	"google.golang.org/grpc"
)

type CardSetResult struct {
	CardSet *cardsetsgrpc.CardSet
	Error   error
}

type Client struct {
	logger     *logger.Logger
	grpcClient cardsetsgrpc.CardSetsClient
}

func New(logger *logger.Logger, grpcClient cardsetsgrpc.CardSetsClient) Client {
	return Client{
		logger:     logger,
		grpcClient: grpcClient,
	}
}

func (c *Client) GetCardSets(ctx context.Context, limit int64) (chan CardSetResult, error) {
	grpcCardSetStream, err := c.grpcClient.GetCardSets(
		ctx,
		&cardsetsgrpc.GetCardSetsIn{Limit: tools.Ptr(limit)},
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
