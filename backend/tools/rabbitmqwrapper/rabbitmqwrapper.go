package rabbitmqwrapper

import (
	"github.com/streadway/amqp"
	"tools/logger"
)

type RabbitMQQueue struct {
	channel *amqp.Channel
	queue   *amqp.Queue
}

func (r *RabbitMQQueue) Close() error {
	if r.channel != nil {
		if err := r.channel.Close(); err != nil {
			return err
		}

		r.channel = nil
	}

	return nil
}

type RabbitMQWrapper struct {
	url        string
	connection *amqp.Connection
	queues     []RabbitMQQueue
}

func New(url string) *RabbitMQWrapper {
	return &RabbitMQWrapper{
		url:    url,
		queues: []RabbitMQQueue{},
	}
}

func (rw *RabbitMQWrapper) Connect() error {
	connection, err := amqp.Dial(rw.url)
	if err != nil {
		return err
	}

	rw.connection = connection

	return err
}

func (rw *RabbitMQWrapper) Stop() error {
	var queueError error
	for i, _ := range rw.queues {
		if err := rw.queues[i].Close(); err != nil {
			queueError = err
		}
	}
	rw.queues = []RabbitMQQueue{}

	if rw.connection != nil {
		if err := rw.connection.Close(); err != nil {
			return err
		}

		rw.connection = nil
	}

	return queueError
}

func (rw *RabbitMQWrapper) QueueDeclare(name string) (*RabbitMQQueue, error) {
	channel, err := rw.connection.Channel()
	if err != nil {
		return nil, err
	}

	queue, err := channel.QueueDeclare(
		name,  // name
		false, // durable
		false, // delete when unused
		false, // exclusive
		false, // no-wait
		nil,   // arguments
	)

	rabbitMQQueue := &RabbitMQQueue{channel, &queue}
	rw.queues = append(rw.queues, *rabbitMQQueue)

	return rabbitMQQueue, nil
}

type RabbitMQApp interface {
	GetLogger() *logger.Logger
	SetRabbitMQWrapper(wrapper *RabbitMQWrapper)
	GetRabbitMQWrapper() *RabbitMQWrapper
}

func SetupRabbitMQ(app RabbitMQApp, url string) error {
	rabbitMQWrapper := New(url)

	if err := rabbitMQWrapper.Connect(); err != nil {
		app.GetLogger().Error.Printf("rabbitMQWrapper.connect() failed: %s\n", err.Error())
		return err
	}

	app.SetRabbitMQWrapper(rabbitMQWrapper)
	return nil
}
