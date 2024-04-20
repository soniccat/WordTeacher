package logger

import (
	"context"
	"errors"
	"log/slog"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestInfo(t *testing.T) {
	ctx := context.Background()

	var sBuilder strings.Builder
	l := New(&sBuilder, slog.LevelDebug)
	l.Info(ctx, "info message", "name1", "val1", "name2", "val2")

	assert.Equal(t, "abc", sBuilder.String())
}

func TestInfoWithError(t *testing.T) {
	ctx := context.Background()
	var sBuilder strings.Builder

	ctx = WithLogParams(ctx, "extraName1", "extraValue1", "extraName2", "extraValue2")
	err := NewLogError(ctx, errors.New("base error"))

	l := New(&sBuilder, slog.LevelDebug)
	l.InfoWithError(ctx, err, "info message", "name1", "val1", "name2", "val2")

	assert.Equal(t, "abc", sBuilder.String())
}
