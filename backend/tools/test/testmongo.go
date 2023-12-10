package test

import (
	"tools/logger"
	"tools/mongowrapper"
)

type TestMongo struct {
	logger       *logger.Logger
	mongoWrapper *mongowrapper.MongoWrapper
}

func NewTestMongo() *TestMongo {
	testMongo := &TestMongo{
		logger: logger.New(true),
	}

	mw, err := mongowrapper.New(
		"mongodb://127.0.0.1:27018/?directConnection=true&replicaSet=rs0",
		false,
	)
	if err != nil {
		panic(err)
	}

	testMongo.mongoWrapper = mw
	return testMongo
}

func (a *TestMongo) GetLogger() *logger.Logger {
	return a.logger
}

func (a *TestMongo) SetMongoWrapper(mw *mongowrapper.MongoWrapper) {
	a.mongoWrapper = mw
}

func (a *TestMongo) GetMongoWrapper() *mongowrapper.MongoWrapper {
	return a.mongoWrapper
}
