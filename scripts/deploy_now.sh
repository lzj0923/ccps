#!/bin/bash
docker exec -i ccps-mysql mysql -uroot -p123456 --force ccps_property_management < /tmp/db_deploy.sql && echo "db-ok"
cd /opt/ccps && docker-compose up -d --force-recreate backend && echo "deploy-ok"
sleep 15
docker ps --filter name=ccps --format '{{.Names}} {{.Status}}'
docker logs --tail 2 ccps-backend
