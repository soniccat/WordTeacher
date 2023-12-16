package test

import (
	"tools/logger"
	"tools/mongowrapper"
)

const (
	ConnectionString = "mongodb://127.0.0.1:27018/?directConnection=true&replicaSet=rs0"
)

type TestMongo struct {
	mongowrapper.MongoEnv
	logger *logger.Logger
}

func NewTestMongo() *TestMongo {
	logger := logger.New(true)
	testMongo := &TestMongo{
		MongoEnv: mongowrapper.NewMongoEnv(logger),
		logger:   logger,
	}

	err := testMongo.SetupMongo(ConnectionString, false)
	if err != nil {
		panic(err)
	}

	return testMongo
}

func (a *TestMongo) GetLogger() *logger.Logger {
	return a.logger
}
