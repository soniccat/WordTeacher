package articles

import (
	"context"
	"tools"
	"tools/logger"

	articlesgrpc "service_articles/pkg/grpc/service_articles/api"

	"google.golang.org/grpc"
)

type Articles struct {
}

type HeadlinesResult struct {
	Headline *articlesgrpc.Headline
	Error    error
}

type Client struct {
	logger     *logger.Logger
	grpcClient articlesgrpc.HeadlinesClient
}

func New(logger *logger.Logger, grpcClient articlesgrpc.HeadlinesClient) Client {
	return Client{
		logger:     logger,
		grpcClient: grpcClient,
	}
}

func (c *Client) GetHeadlines(
	ctx context.Context,
	category int32,
	limit int64,
) (chan HeadlinesResult, error) {
	grpcHeadlineStream, err := c.grpcClient.GetHeadlines(
		ctx,
		&articlesgrpc.GetHeadlinesIn{
			Category: articlesgrpc.Category(category),
			Limit:    tools.Ptr(limit),
		},
		grpc.EmptyCallOption{},
	)

	if err != nil {
		return nil, logger.WrapError(ctx, err)
	}

	headlineChan := make(chan HeadlinesResult)
	go func() {
		for {
			h, err := grpcHeadlineStream.Recv()
			if err != nil {
				headlineChan <- HeadlinesResult{Error: logger.WrapError(ctx, err)}
				close(headlineChan)
				break
			} else {
				headlineChan <- HeadlinesResult{Headline: h}
			}
		}
	}()

	return headlineChan, nil
}
