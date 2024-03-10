package service_models

type ErrorInvalidToken struct {
	s string
}

func NewErrorInvalidToken(str string) *ErrorInvalidToken {
	return &ErrorInvalidToken{
		s: str,
	}
}

func (e *ErrorInvalidToken) Error() string { return e.s }
