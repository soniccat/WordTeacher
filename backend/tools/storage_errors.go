package tools

import "fmt"

// InvalidIdError

type InvalidIdError struct {
	id  string
	err error
}

func NewInvalidIdError(id string, err error) InvalidIdError {
	return InvalidIdError{
		id:  id,
		err: err,
	}
}

func (e InvalidIdError) Error() string {
	return fmt.Sprintf("invalid id %s: %v", e.id, e.err)
}

func (e InvalidIdError) Unwrap() error {
	return e.err
}

// InvalidInputError

type InvalidArgumentError struct {
	name        string
	input       interface{}
	description string
	innerError  error
}

func NewInvalidArgumentError(name string, input interface{}, description string, innerError error) InvalidArgumentError {
	return InvalidArgumentError{
		name:        name,
		input:       input,
		description: description,
		innerError:  innerError,
	}
}

func (e InvalidArgumentError) Error() string {
	return fmt.Sprintf("invalid argument \"%s\": %v. %s, innerError: %v", e.name, e.input, e.description, e.innerError)
}

func (e InvalidArgumentError) Unwrap() error {
	return e.innerError
}
