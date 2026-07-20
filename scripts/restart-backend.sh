#!/bin/bash
docker stop ccps-backend 2>/dev/null
docker rm ccps-backend 2>/dev/null
docker run -d --name ccps-backend --restart unless-stopped --network ccps_default \
  -e 'DB_URL=jdbc:mysql://mysql:3306/ccps_property_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
  -e DB_USERNAME=root -e DB_PASSWORD=123456 -e TZ=Asia/Shanghai \
  -p 127.0.0.1:8082:8080 -v backend_uploads:/app/uploads ccps-backend
echo "Container status:"
docker ps --filter name=ccps-backend --format '{{.Names}}  {{.Status}}'
