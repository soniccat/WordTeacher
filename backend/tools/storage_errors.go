package tools

import "fmt"

type InvalidIdError struct {
	id string
}

func NewInvalidIdError(id string) InvalidIdError {
	return InvalidIdError{
		id: id,
	}
}

func (e InvalidIdError) Error() string {
	return fmt.Sprintf("invalid id %s", e.id)
}
