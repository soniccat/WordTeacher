package service_models

type ErrorInvalidToken struct {
	innerErr error
}

func NewErrorInvalidToken(err error) *ErrorInvalidToken {
	return &ErrorInvalidToken{
		innerErr: err,
	}
}

func (e *ErrorInvalidToken) Error() string {
	return e.innerErr.Error()
}

func (e *ErrorInvalidToken) Unwrap() error {
	return e.innerErr
}
