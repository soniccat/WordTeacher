package test

import (
	"bytes"
	"compress/gzip"
	"encoding/json"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"tools/logger"

	"github.com/google/uuid"
)

type BaseTestSuite struct {
	t                *testing.T
	logger           *logger.Logger
	testTimeProvider TimeProvider
}

func (b *BaseTestSuite) TestTimeProvider() *TimeProvider {
	return &b.testTimeProvider
}

func (b *BaseTestSuite) Logger() *logger.Logger {
	if b.logger == nil {
		b.logger = logger.New(os.Stdout, slog.LevelDebug)
	}

	return b.logger
}

func (b *BaseTestSuite) CreateUUID() uuid.UUID {
	cardSetCreationIdUUID, err := uuid.NewUUID()
	if err != nil {
		b.t.Fatal(err)
	}
	return cardSetCreationIdUUID
}

func TestPostRequest[T any](path string, input T) *http.Request {
	req, err := http.NewRequest("POST", path, bytes.NewReader(TestMarshal(input)))
	if err != nil {
		panic(err)
	}

	return req
}

func TestMarshal[T any](t T) []byte {
	body, err := json.Marshal(t)
	if err != nil {
		panic(err)
	}

	return body
}

type RawResponse struct {
	Value  json.RawMessage `json:"value"`
	Status string          `json:"status"`
}

func TestReadResponse[T any](writer *httptest.ResponseRecorder) *T {
	var rawResponse RawResponse

	reader, err := gzip.NewReader(writer.Result().Body)
	if err != nil {
		panic(err)
	}

	body, err := io.ReadAll(reader)
	if err != nil {
		panic(err)
	}

	err = json.Unmarshal(body, &rawResponse)
	if err != nil {
		panic(err)
	}

	var pullResponse T
	err = json.Unmarshal(rawResponse.Value, &pullResponse)
	if err != nil {
		panic(err)
	}

	return &pullResponse
}
