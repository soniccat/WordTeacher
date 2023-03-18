package test

import (
	"bytes"
	"encoding/json"
	"github.com/google/uuid"
	"io"
	"net/http"
	"net/http/httptest"
	"testing"
)

type BaseTestSuite struct {
	t *testing.T
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
	body, err := io.ReadAll(writer.Result().Body)
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
