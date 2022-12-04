# we will put our integration testing in this path
INTEGRATION_TEST_PATH?=./it

# set of env variables that you need for testing
ENV_LOCAL_TEST=\
  POSTGRES_PASSWORD=mysecretpassword \
  POSTGRES_DB=myawesomeproject \
  POSTGRES_HOST=postgres \
  POSTGRES_USER=postgres

# this command will start a docker components that we set in docker-compose.yml
docker.start.components:
  docker-compose up -d --remove-orphans postgres;

# shutting down docker components
docker.stop:
  docker-compose down;

# this command will trigger integration test
# INTEGRATION_TEST_SUITE_PATH is used for run specific test in Golang, if it's not specified
# it will run all tests under ./it directory
test.integration:
  $(ENV_LOCAL_TEST) \
  go test -tags=integration $(INTEGRATION_TEST_PATH) -count=1 -run=$(INTEGRATION_TEST_SUITE_PATH)

# this command will trigger integration test with verbose mode
test.integration.debug:
  $(ENV_LOCAL_TEST) \
  go test -tags=integration $(INTEGRATION_TEST_PATH) -count=1 -v -run=$(INTEGRATION_TEST_SUITE_PATH)

