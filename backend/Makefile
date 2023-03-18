# we will put our integration testing in this path
INTEGRATION_TEST_PATH?=./it

# set of env variables that you need for testing
ENV_LOCAL_TEST=\
  POSTGRES_PASSWORD=mysecretpassword \
  POSTGRES_DB=myawesomeproject \
  POSTGRES_HOST=postgres \
  POSTGRES_USER=postgres

RUNTESTSSTEPS := downtest removetestvolumes uptest runtests_step up

runtests: $(RUNTESTSSTEPS)

runtests_step:
	sleep 2 && cd models && go test -tags=integration ./... -count=1 -run=

removetestvolumes:
	docker volume rm backend_mongodb_test backend_mongoconfig_test backend_redis_test || true

up:
	docker compose --env-file ./prod.env up -d

stop:
	docker compose --env-file ./prod.env stop

down:
	docker compose --env-file ./prod.env down

uptest:
	docker compose --env-file ./test.env up -d

stoptest:
	docker compose --env-file ./test.env stop

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

# service_auth

authup:
	docker stop auth || true
	docker rm auth || true
	docker rmi auth || true
	make up

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

# service_cardsets

cardsetsup:
	docker stop cardsets || true
	docker rm cardsets || true
	docker rmi cardsets || true
	make up

cardsetsstop:
	docker container stop cardsets

cardsetsrm: cardsetsstop
	docker container rm cardsets
	docker image rm cardsets

cardsetsbuild:
	docker build -t cardsets -f ./Auth.Dockerfile .
	docker container create -p 4001:4001 --name cardsets cardsets

cardsetsrun:
	docker start cardsets

#
#cardsetsstop:
#	docker container stop service_cardsets
#
#cardsetsrm: cardsetsstop
#	docker container rm service_cardsets
#	docker image rm service_cardsets
#
#cardsetsbuild:
#	docker build -t service_cardsets -f ./CardSets.Dockerfile .
#	docker container create -p 4001:4001 --name service_cardsets service_cardsets
#
#cardsetsrun:
#	docker start service_cardsets
#
#cardsetsclearandbuild: cardsetsrm cardsetsbuild
#cardsetsclearandrun: cardsetsclearandbuild cardsetsrun
