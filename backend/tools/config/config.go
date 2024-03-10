package config

import (
	"encoding/json"
	"io"
	"os"
)

func ParseJsonConfig(path string, data any) error {
	file, err := os.Open(path)
	if err != nil {
		return err
	}
	bytes, err := io.ReadAll(file)
	if err != nil {
		return err
	}

	err = json.Unmarshal(bytes, data)
	if err != nil {
		return err
	}

	return nil
}

func RequireJsonConfig(path string, data any) {
	err := ParseJsonConfig(path, data)
	if err != nil {
		panic(err)
	}
}
