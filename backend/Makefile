# we will put our integration testing in this path
INTEGRATION_TEST_PATH?=./it

# set of env variables that you need for testing
ENV_LOCAL_TEST=\
  POSTGRES_PASSWORD=mysecretpassword \
  POSTGRES_DB=myawesomeproject \
  POSTGRES_HOST=postgres \
  POSTGRES_USER=postgres

up:
	docker compose --env-file ./prod.env up -d

down:
	docker compose --env-file ./prod.env down

uptest:
	docker compose --env-file ./test.env -f ./docker-compose.yml up -d

downtest:
	docker compose --env-file ./test.env down

# this command will trigger integration test
# INTEGRATION_TEST_SUITE_PATH is used for run specific test in Golang, if it's not specified
# it will run all tests under ./it directory
test.integration:
	$(ENV_LOCAL_TEST) \
	go test -tags=integration ./models/user -count=1 -run=$(INTEGRATION_TEST_SUITE_PATH)

# this command will trigger integration test with verbose mode
test.integration.debug:
	$(ENV_LOCAL_TEST) \
	go test -tags=integration $(INTEGRATION_TEST_PATH) -count=1 -v -run=$(INTEGRATION_TEST_SUITE_PATH)

# auth

authstop:
	docker container stop auth

authrm: authstop
	docker container rm auth
	docker image rm auth

authbuild:
	docker build -t auth -f ./Auth.Dockerfile .
	docker container create -p 4000:4000 --name auth auth

authrun:
	docker start auth

authclearandbuild: authrm authbuild
authclearandrun: authclearandbuild authrun

# cardsets

cardsetsstop:
	docker container stop cardsets

cardsetsrm: cardsetsstop
	docker container rm cardsets
	docker image rm cardsets

cardsetsbuild:
	docker build -t cardsets -f ./CardSets.Dockerfile .
	docker container create -p 4001:4001 --name cardsets cardsets

cardsetsrun:
	docker start cardsets

cardsetsclearandbuild: cardsetsrm cardsetsbuild
cardsetsclearandrun: cardsetsclearandbuild cardsetsrun
