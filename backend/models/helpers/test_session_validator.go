package helpers

import (
	"errors"
	"models"
	"net/http"
)

type MockSessionValidatorResponse struct {
	AuthToken            *models.UserAuthToken
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

func (tsv *MockSessionValidator) Validate(r *http.Request) (*models.UserAuthToken, *ValidateSessionError) {
	response := tsv.ResponseProvider()
	return response.AuthToken, response.ValidateSessionError
}
