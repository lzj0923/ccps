# Meta WhatsApp Webhook 回调验证失败 - 修复报告

## BUG描述

- 页面/功能：Meta 开发者后台配置 WhatsApp Webhook
- 操作步骤：填写公开回调网址与验证权杖后点击“验证并储存”
- 期望结果：Meta 验证成功并保存 Webhook
- 实际结果：Meta 提示无法验证回调网址或验证权杖

## 复现步骤

1. 通过 Cloudflare Tunnel 请求 `/api/webhooks/whatsapp`。
2. 首次请求返回 `401 Authentication required`，表明运行中的后端仍是旧进程。
3. 部署新代码并重启后，请求返回 `503 WhatsApp webhook verify token is not configured`。

## 根因分析

- 表象：Meta 无法验证回调地址。
- 根因一：Tunnel 最初代理的是尚未加载 Webhook 公开路由的旧后端进程。
- 根因二：重启新后端时没有设置 `WHATSAPP_WEBHOOK_VERIFY_TOKEN`，导致服务无法比对 Meta 提交的验证权杖。

## 修复方案

- 重启后端，使 `/api/webhooks/whatsapp` 及其拦截器放行配置生效。
- 在启动后端的同一环境中设置 `WHATSAPP_WEBHOOK_VERIFY_TOKEN`。
- Meta 后台的“验证权杖”填写与后端完全相同的值。

## 单元测试

- 测试文件：`backend/src/test/java/com/ccps/backend/controller/WhatsAppWebhookControllerTest.java`
- 覆盖正确/错误权杖、正确/错误签名、缺失 App Secret 等场景。
- 测试结果：5 项全部通过。

## 手动验证步骤

1. 保持后端与 Cloudflare Tunnel 运行。
2. 从公网发送包含 `hub.mode`、`hub.verify_token` 和 `hub.challenge` 的 GET 请求。
3. 确认返回 `HTTP 200`、`Content-Type: text/plain`，正文与 `hub.challenge` 完全一致。
4. 在 Meta 后台点击“验证并储存”。

## 影响范围

- 直接影响：WhatsApp Webhook 回调地址验证。
- 间接影响：后续 WhatsApp 消息及状态事件接收。
- 验证结果：公网请求已返回 `HTTP 200`，challenge 原样返回。
