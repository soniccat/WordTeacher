RUNTESTSSTEPS := downtest removetestvolumes uptest runtests_step up

runtests: $(RUNTESTSSTEPS)

runtests_step:
	sleep 2 && cd models && go test -tags=integration ./... -count=1 -run=

removetestvolumes:
	docker volume rm backend_mongodb_test backend_mongoconfig_test backend_redis_test || true

clean:
	docker rmi cardsets auth cardsetsearch dict articles dashboard

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
# uncomment and set valuer for variables in ./cron/crontab
# update domain in ./cron/wordteacher_renew_cert.sh to yours
cronsetup:
	sudo mkdir -p /var/spool/cron/crontabs/$(USER) &&\
	sudo ln -s ./cron/crontab /var/spool/cron/crontabs/$(USER)/wordteacher_crontab &&\
	sudo mkdir /Users/Shared/WordTeacher/certbot/logs &&\
	sudo mkdir /Users/Shared/WordTeacher/backups/mongo/logs

cronup:
	sudo crontab /var/spool/cron/crontabs/$(USER)/wordteacher_crontab

crondown:
	sudo crontab -r

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
	docker container create -p 4003:4003 --name dict dict

dictrun:
	docker start dict

# service_articles

articlesup:
	docker stop articles || true
	docker rm articles || true
	docker rmi articles || true
	make up

articlesstop:
	docker container stop articles

articlesrm: articlestop
	docker container rm articles
	docker image rm articles

articlesbuild:
	docker build -t articles -f ./Articles.Dockerfile .
	docker container create -p 4004:4004 --name articles articles

articlesrun:
	docker start articles

# service_dashboard

dashboardup:
	docker stop dashboard || true
	docker rm dashboard || true
	docker rmi dashboard || true
	make up

dashboardstop:
	docker container stop dashboard

dashboardrm: articlestop
	docker container rm dashboard
	docker image rm dashboard

dashboardbuild:
	docker build -t dashboard -f ./Dashboard.Dockerfile .
	docker container create -p 4005:4005 --name dashboard dashboard

dashboardrun:
	docker start dashboard
