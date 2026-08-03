#!/bin/bash
# Cloudflare Tunnel setup for CCPS

cat > /etc/systemd/system/cloudflared.service << 'EOF'
[Unit]
Description=Cloudflare Tunnel for CCPS
After=network.target

[Service]
Type=simple
ExecStart=/usr/local/bin/cloudflared tunnel --url http://localhost:8080
Restart=always
RestartSec=5
StandardOutput=append:/var/log/cloudflared.log
StandardError=append:/var/log/cloudflared.log

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable cloudflared
systemctl start cloudflared
sleep 5
echo "=== Tunnel URL ==="
grep -oP 'https://[a-zA-Z0-9.-]+\.trycloudflare\.com' /var/log/cloudflared.log | tail -1
echo "=== Service Status ==="
systemctl status cloudflared --no-pager | head -5
