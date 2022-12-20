package test

import (
	"models/logger"
	"models/mongowrapper"
	"models/tools"
)

type TestMongo struct {
	logger       *logger.Logger
	mongoWrapper *mongowrapper.MongoWrapper
}

func New() *TestMongo {
	testMongo := &TestMongo{
		logger: logger.New(true),
	}
	err := mongowrapper.SetupMongo(
		testMongo,
		tools.Ptr("mongodb://127.0.0.1:27018/?directConnection=true&replicaSet=rs0"),
		tools.Ptr(false),
	)
	if err != nil {
		panic(err)
	}

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
