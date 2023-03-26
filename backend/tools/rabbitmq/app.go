package rabbitmq

import (
	"time"
	"tools/logger"
)

type App interface {
	GetLogger() *logger.Logger
	SetRabbitMQWrapper(rabbitMQ *RabbitMQ)
	GetRabbitMQWrapper() *RabbitMQ
}

func SetupApp(app App, url string) (err error) {
	rabbitMQ := New(url)

	for i := 0; i < 10; i++ {
		if err = rabbitMQ.Connect(); err != nil {
			app.GetLogger().Error.Printf("rabbitMQ.connect() failed: %s\n", err.Error())
			time.Sleep(5 * time.Second)
		}
	}

	if err != nil {
		return err
	}

	app.SetRabbitMQWrapper(rabbitMQ)
	return nil
}
