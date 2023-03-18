package models

import (
	"errors"
	"net/http"
)

type MockSessionValidatorResponse struct {
	AuthToken            *UserAuthToken
	ValidateSessionError *ValidateSessionError
}

type MockSessionValidator struct {
	ResponseProvider func() MockSessionValidatorResponse
}

func NewMockSessionValidator() *MockSessionValidator {
	return &MockSessionValidator{
		func() MockSessionValidatorResponse {
			return MockSessionValidatorResponse{nil, NewValidateSessionError(http.StatusInternalServerError, errors.New("MockSessionValidator error"))}
		},
	}
}

func (tsv *MockSessionValidator) Validate(r *http.Request) (*UserAuthToken, *ValidateSessionError) {
	response := tsv.ResponseProvider()
	return response.AuthToken, response.ValidateSessionError
}
