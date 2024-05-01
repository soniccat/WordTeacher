# type your user name here `echo $USER` before calling make cronsetup
CRONUSER=aglushkov

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

clean:
	docker rmi cardsets auth cardsetsearch dict

up:
	docker compose --env-file ./prod.env up -d

reup: down clean up

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

# need to enable running docker without sudo according that guide https://docs.docker.com/engine/install/linux-postinstall/
# update domain in this file to yours
# set CRONUSER to your $USER
cronsetup:
	mkdir -p /var/spool/cron/crontabs/$(CRONUSER) &&\
	mkdir -p ~/wordteacher &&\
	cp ./cron/crontab /var/spool/cron/crontabs/$(CRONUSER)/wordteacher_crontab &&\
	cp ./cron/wordteacher_renew_cert.sh ~/wordteacher/wordteacher_renew_cert.sh &&\
	cp ./prod.env ~/wordteacher/prod.env

cronup:
	crontab /var/spool/cron/crontabs/$(CRONUSER)/wordteacher_crontab

crondown:
	crontab -r

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
	docker build -t cardsets -f ./CardSets.Dockerfile .
	docker container create -p 4001:4001 --name cardsets cardsets

cardsetsrun:
	docker start cardsets

# service_cardsetsearch

cardsetsearchup:
	docker stop cardsetsearch || true
	docker rm cardsetsearch || true
	docker rmi cardsetsearch || true
	make up

cardsetsearchstop:
	docker container stop cardsetsearch

cardsetsearchrm: cardsetsstop
	docker container rm cardsetsearch
	docker image rm cardsetsearch

cardsetsearchbuild:
	docker build -t cardsetsearch -f ./CardSetSearch.Dockerfile .
	docker container create -p 4002:4002 --name cardsetsearch cardsetsearch

cardsetsearchrun:
	docker start cardsetsearch

# service_dict

dictup:
	docker stop dict || true
	docker rm dict || true
	docker rmi dict || true
	make up

dictstop:
	docker container stop dict

dictrm: dictstop
	docker container rm dict
	docker image rm dict

dictbuild:
	docker build -t dict -f ./Dict.Dockerfile .
	docker container create -p 4001:4001 --name dict dict

dictrun:
	docker start dict

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
