package logger

import (
	"context"
	"encoding/json"
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

	var raw map[string]string
	s := sBuilder.String()
	print(s)
	jsonErr := json.Unmarshal([]byte(s), &raw)

	assert.Nil(t, jsonErr)
	assert.Equal(t, "INFO", raw["level"])
	assert.Equal(t, "info message", raw["msg"])
	assert.Equal(t, "val1", raw["name1"])
	assert.Equal(t, "val2", raw["name2"])
}

func TestInfoWithWrappingContextTwice(t *testing.T) {
	ctx := context.Background()
	ctx = WrapContext(ctx, "a1", "a2", "b1", "b2")
	ctx = WrapContext(ctx, "c1", "c2", "d1", "d2")

	var sBuilder strings.Builder
	l := New(&sBuilder, slog.LevelDebug)
	l.Info(ctx, "info message", "name1", "val1", "name2", "val2")

	var raw map[string]string
	s := sBuilder.String()
	print(s)
	jsonErr := json.Unmarshal([]byte(s), &raw)

	assert.Nil(t, jsonErr)
	assert.Equal(t, "INFO", raw["level"])
	assert.Equal(t, "info message", raw["msg"])
	assert.Equal(t, "val1", raw["name1"])
	assert.Equal(t, "val2", raw["name2"])
	assert.Equal(t, "a2", raw["a1"])
	assert.Equal(t, "b2", raw["b1"])
	assert.Equal(t, "c2", raw["c1"])
	assert.Equal(t, "d2", raw["d1"])
}

func TestInfoWithError(t *testing.T) {
	ctx := context.Background()
	var sBuilder strings.Builder

	ctx = WrapContext(ctx, "extraName1", "extraValue1", "extraName2", "extraValue2")
	err := WrapError(ctx, errors.Join(errors.New("error1"), errors.New("error2")))

	l := New(&sBuilder, slog.LevelDebug)
	l.InfoWithError(ctx, err, "info message", "name1", "val1", "name2", "val2")

	var raw map[string]string
	s := sBuilder.String()
	print(s)
	jsonErr := json.Unmarshal([]byte(s), &raw)

	assert.Nil(t, jsonErr)
	assert.Equal(t, "INFO", raw["level"])
	assert.Equal(t, "info message", raw["msg"])
	assert.Equal(t, "error1\nerror2", raw["error"])
	assert.Equal(t, "val1", raw["name1"])
	assert.Equal(t, "val2", raw["name2"])
	assert.Equal(t, "extraValue1", raw["extraName1"])
	assert.Equal(t, "extraValue2", raw["extraName2"])
	assert.NotNil(t, raw["stack"])
}
