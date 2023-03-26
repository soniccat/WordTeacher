package rabbitmq

import (
	"context"
	amqp "github.com/rabbitmq/amqp091-go"
	"time"
)

const Timeout = 5 * time.Second

type RabbitMQ struct {
	url        string
	connection *amqp.Connection
	queues     []Queue
}

type Queue struct {
	channel *amqp.Channel
	queue   *amqp.Queue
}

func (r *Queue) Publish(body []byte) error {
	ctx, cancel := context.WithTimeout(context.Background(), Timeout)
	defer cancel()

	return r.channel.PublishWithContext(ctx,
		"",           // exchange
		r.queue.Name, // routing key
		false,        // mandatory
		false,        // immediate
		amqp.Publishing{
			ContentType: "text/plain",
			Body:        body,
		},
	)
}

func (r *Queue) Consume() (<-chan amqp.Delivery, error) {
	return r.channel.Consume(
		r.queue.Name, // queue
		"",           // consumer
		false,        // auto-ack
		false,        // exclusive
		false,        // no-local
		false,        // no-wait
		nil,          // args
	)
}

func (r *Queue) Close() error {
	if r.channel != nil {
		if err := r.channel.Close(); err != nil {
			return err
		}

		r.channel = nil
	}

	return nil
}

func New(url string) *RabbitMQ {
	return &RabbitMQ{
		url:    url,
		queues: []Queue{},
	}
}

func (rw *RabbitMQ) Connect() error {
	connection, err := amqp.Dial(rw.url)
	if err != nil {
		return err
	}

	rw.connection = connection

	return err
}

func (rw *RabbitMQ) Stop() error {
	var queueError error
	for i, _ := range rw.queues {
		if err := rw.queues[i].Close(); err != nil {
			queueError = err
		}
	}
	rw.queues = []Queue{}

	if rw.connection != nil {
		if err := rw.connection.Close(); err != nil {
			return err
		}

		rw.connection = nil
	}

	return queueError
}

func (rw *RabbitMQ) QueueDeclare(name string) (*Queue, error) {
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

	rabbitMQQueue := &Queue{channel, &queue}
	rw.queues = append(rw.queues, *rabbitMQQueue)

	return rabbitMQQueue, nil
}
