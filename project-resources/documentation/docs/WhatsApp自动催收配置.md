# WhatsApp 自动催收配置

## 功能范围

系统会在租金逾期流程的四个阶段建立 WhatsApp 发送任务：第一次提醒、第二次提醒、最终提醒和终止通知。只有租客目录中已勾选“启用 WhatsApp 自动催收”并确认取得租客明确同意的号码才会发送。

发送任务由后端每 15 秒处理一次。Meta 接受消息后保存 `wamid`，Webhook 后续将状态更新为已发送、已送达、已读或失败。终止通知只发送通知，不会自动结束租约或停用门禁。

## 1. 执行数据库迁移

在现有数据库执行：

`database/migrate_whatsapp_notifications.sql`

该迁移只新增租客 WhatsApp 授权表及发送尝试表，不删除现有数据。

## 2. 在 Meta 创建模板

在 WhatsApp Manager 创建四个 `UTILITY` 模板。模板名称建议如下：

- `ccps_rent_first_reminder`
- `ccps_rent_second_reminder`
- `ccps_rent_final_reminder`
- `ccps_rent_termination_notice`

四个模板均使用同样的 7 个正文变量，正文可填写：

```text
您好 {{1}}，这是 CCPS 的{{7}}。您承租的 {{2}}，{{3}} 租金尚欠 {{4}}，到期日为 {{5}}，目前已逾期 {{6}} 天。如已付款，请联系管理人员核对。
```

变量顺序固定为：租客姓名、房产与单元、账单月份、未缴金额、到期日、逾期天数、催收阶段。模板通过 Meta 审核后再启用发送。

## 3. 启动后端前设置环境变量

PowerShell 示例：

```powershell
$env:WHATSAPP_ENABLED = 'true'
$env:WHATSAPP_GRAPH_API_VERSION = 'v26.0'
$env:WHATSAPP_PHONE_NUMBER_ID = '1331186120071007'
$env:WHATSAPP_ACCESS_TOKEN = '粘贴 Meta 访问口令，不是电话号码或验证码'
$env:WHATSAPP_DEFAULT_COUNTRY_CODE = '60'
$env:WHATSAPP_TEMPLATE_LANGUAGE = 'zh_CN'
$env:WHATSAPP_TEMPLATE_FIRST_REMINDER = 'ccps_rent_first_reminder'
$env:WHATSAPP_TEMPLATE_SECOND_REMINDER = 'ccps_rent_second_reminder'
$env:WHATSAPP_TEMPLATE_FINAL_REMINDER = 'ccps_rent_final_reminder'
$env:WHATSAPP_TEMPLATE_TERMINATION_NOTICE = 'ccps_rent_termination_notice'
$env:WHATSAPP_WEBHOOK_VERIFY_TOKEN = '13094413501'
$env:META_APP_SECRET = 'Meta 应用程序密钥'
```

环境变量只对当前终端生效，因此必须在同一个终端中设置变量并启动后端。访问口令和应用程序密钥不得提交到 Git、前端代码或录屏中。

## 4. Meta Webhook

- 回调网址：`https://你的域名/api/webhooks/whatsapp`
- 验证权杖：与 `WHATSAPP_WEBHOOK_VERIFY_TOKEN` 完全一致
- 验证成功后订阅字段：`messages`

Quick Tunnel 每次重启地址都会变化。用于正式审核和上线时应换成固定 HTTPS 域名，并同步更新 Meta Webhook 与隐私政策网址。

## 5. 系统操作与审核录屏

1. 进入“租客目录”，编辑测试租客，填写带国家码的手机号。
2. 勾选“启用 WhatsApp 自动催收”和租客同意确认框，保存。
3. 进入“提醒／租金催缴”，选择已经达到催收阶段的逾期账单。
4. 点击发送当前催收阶段；系统同时建立站内、邮件（有邮箱时）和 WhatsApp 任务。
5. 打开“发送结果”，展示 WhatsApp 从待发送变为已发送/已送达/已读。
6. 手机端展示收到的模板消息。

录屏不要显示 Meta 访问口令、应用程序密钥、数据库密码或终端环境变量值。

## 6. 常见问题

- 一直“待发送”：检查 `WHATSAPP_ENABLED`、Phone Number ID、访问口令和四个模板名，修改后重启后端。
- Meta 返回模板不存在或语言错误：模板名和语言必须与 WhatsApp Manager 中已批准版本完全一致。
- 没有生成 WhatsApp 记录：租客必须是启用状态、租约必须有效，并且已经在租客目录确认授权。
- 消息失败：在“发送结果”查看 Meta 错误信息；修复配置后点击重试。
