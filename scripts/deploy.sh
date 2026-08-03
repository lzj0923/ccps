#!/bin/bash
set -e
echo "=== CCPS Deploy $(date) ==="

# 1. Stop old container
echo "[1/5] Stopping old backend..."
docker stop ccps-backend 2>/dev/null || true
docker rm ccps-backend 2>/dev/null || true

# 2. Rebuild image (no cache)
echo "[2/5] Building new image..."
cd /opt/ccps
docker build --no-cache -t ccps-backend -f docker/backend/Dockerfile .

# 3. Start new container
echo "[3/5] Starting backend..."
docker run -d --name ccps-backend --restart unless-stopped --network ccps_default \
  --env-file /opt/ccps/.env \
  -e 'DB_URL=jdbc:mysql://mysql:3306/ccps_property_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
  -e DB_USERNAME=root -e DB_PASSWORD=123456 -e TZ=Asia/Shanghai \
  -p 127.0.0.1:8082:8080 -v backend_uploads:/app/uploads ccps-backend

# 4. Wait for healthy
echo "[4/5] Waiting for backend to start..."
sleep 8

# 5. Run migrations
echo "[5/5] Running DB migrations..."
for f in /tmp/migrate_rental_mandates.sql /tmp/migrate_property_handovers.sql /tmp/migrate_owner_contact_fields.sql /tmp/backfill_legacy_rental_mandates.sql; do
  echo "  -> $f"
  docker exec -i ccps-mysql sh -c "mysql -uroot -p\$MYSQL_ROOT_PASSWORD ccps_property_management" < "$f" && echo "     OK" || echo "     FAIL (may already exist)"
done

echo "=== Deploy Complete ==="
docker ps --filter name=ccps --format 'table {{.Names}}\t{{.Status}}'
