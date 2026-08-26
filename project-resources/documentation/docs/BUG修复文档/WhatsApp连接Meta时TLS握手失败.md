# WhatsApp 连接 Meta 时 TLS 握手失败 - 修复报告

## BUG描述
- 页面/功能：WhatsApp 催收消息发送与重试。
- 操作步骤：对已有 WhatsApp 投递点击“重试”。
- 期望结果：后端通过 Meta Graph API 发送消息。
- 实际结果：投递状态变为 `unknown`，提示远端终止 TLS 握手。

## 复现步骤
1. 使用当前 Java 21 运行时直连 `https://graph.facebook.com/v26.0/`。
2. 得到 `SSLHandshakeException: Remote host terminated the handshake`。
3. 显式使用本机代理 `127.0.0.1:7897` 执行相同请求，成功取得 Meta HTTP 响应。

## 根因分析
- 表象：Meta Graph API 请求结果未知。
- 根因：本机访问 Meta 必须经过 HTTPS 代理；Java 后端的 `RestClient` 没有自动读取 `HTTPS_PROXY`，因此走了被中断的直连链路。
- 传输失败发生在取得 HTTP 响应之前，系统将状态标记为 `unknown`，避免误判失败后自动重复发送。

## 修复方案
- 修改 `backend/src/main/java/com/ccps/backend/service/WhatsAppGraphClient.java`。
- 为 WhatsApp Graph 客户端增加专用 HTTP 代理支持。
- 修改 `backend/src/main/resources/application.yml`。
- 新增 `WHATSAPP_PROXY_URL`，默认回退读取 `HTTPS_PROXY` 或 `HTTP_PROXY`。

## 单元测试
- 测试文件：`backend/src/test/java/com/ccps/backend/service/WhatsAppGraphClientProxyTest.java`
- 覆盖代理 URL 解析及无代理配置分支。
- WhatsApp、提醒重试相关 8 项测试全部通过。

## 手动验证步骤
1. 在原后端终端设置 `WHATSAPP_PROXY_URL=http://127.0.0.1:7897`。
2. 重启后端，保留原 Meta 访问令牌等环境变量。
3. 回到发送结果页面，对 `unknown` 记录点击“重试”。
4. 确认不再出现 TLS 握手错误，并查看 Meta 返回的发送状态。

## 影响范围
- 直接影响：后端到 Meta Graph API 的 HTTPS 请求。
- 间接影响：无；代理仅应用于 WhatsApp 客户端。
- 回归测试结果：通过。
