package test

import (
	"errors"
	"models/user"
	"models/userauthtoken"
	"net/http"
)

type MockSessionValidatorResponse[T any] struct {
	Input                *T
	AuthToken            *userauthtoken.UserAuthToken
	ValidateSessionError *user.ValidateSessionError
}

type MockSessionValidator[T any] struct {
	ResponseProvider func() MockSessionValidatorResponse[T]
}

func NewMockSessionValidator[T any]() *MockSessionValidator[T] {
	return &MockSessionValidator[T]{
		func() MockSessionValidatorResponse[T] {
			return MockSessionValidatorResponse[T]{nil, nil, user.NewValidateSessionError(http.StatusInternalServerError, errors.New("mock error"))}
		},
	}
}

func (tsv *MockSessionValidator[T]) Validate(r *http.Request) (*T, *userauthtoken.UserAuthToken, *user.ValidateSessionError) {
	response := tsv.ResponseProvider()
	return response.Input, response.AuthToken, response.ValidateSessionError
}
