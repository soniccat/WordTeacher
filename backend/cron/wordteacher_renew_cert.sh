#/bin/bash

set -e
logs="/Users/Shared/WordTeacher/certbot/logs"
exec >> $logs
exec 2>&1

docker compose -f "$WT_PATH/backend/docker-compose.yml" --env-file "$WT_PATH/backend/prod.env" run --rm certbot certonly --webroot --webroot-path /var/www/certbot/ -d 'aglushkov.ru,aglushkov.com' --keep-until-expiring && \
docker exec apigateway sh -c 'nginx -s reload'
