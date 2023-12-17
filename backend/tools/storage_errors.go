package tools

import "fmt"

// InvalidIdError

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

// InvalidInputError

type InvalidArgumentError struct {
	name        string
	input       interface{}
	description string
}

func NewInvalidArgumentError(name string, input interface{}, description string) InvalidArgumentError {
	return InvalidArgumentError{
		name:        name,
		input:       input,
		description: description,
	}
}

func (e InvalidArgumentError) Error() string {
	return fmt.Sprintf("invalid argument \"%s\": %v. %s", e.name, e.input, e.description)
}
