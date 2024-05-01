
docker compose --env-file ~/wordteacher/prod.env run --rm certbot certonly --webroot --webroot-path /var/www/certbot/ -d aglushkov.ru --keep-until-expiring && \
docker exec apigateway sh -c 'nginx -s reload'
