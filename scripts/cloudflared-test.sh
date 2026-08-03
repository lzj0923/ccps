#!/bin/bash
# Cloudflare Tunnel temporary test - no persistence

# Kill any existing tunnel
pkill -f "cloudflared tunnel" 2>/dev/null || true

# Start temporary tunnel
nohup /usr/local/bin/cloudflared tunnel --url http://localhost:8080 --no-autoupdate > /var/log/cloudflared.log 2>&1 &

# Wait for URL
sleep 6
echo "=== Tunnel URL ==="
grep -oP 'https://[a-zA-Z0-9.-]+\.trycloudflare\.com' /var/log/cloudflared.log | tail -1
