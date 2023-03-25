package rabbitmq

import (
	"tools/logger"
)

type App interface {
	GetLogger() *logger.Logger
	SetRabbitMQWrapper(rabbitMQ *RabbitMQ)
	GetRabbitMQWrapper() *RabbitMQ
}

func SetupApp(app App, url string) error {
	rabbitMQ := New(url)

	if err := rabbitMQ.Connect(); err != nil {
		app.GetLogger().Error.Printf("rabbitMQ.connect() failed: %s\n", err.Error())
		return err
	}

	app.SetRabbitMQWrapper(rabbitMQ)
	return nil
}
