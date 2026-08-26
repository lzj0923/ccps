# WhatsApp 重试手机号为空 - 修复报告

## BUG描述
- 页面/功能：自动提醒的 WhatsApp 发送结果重试。
- 操作步骤：先建立租金逾期提醒，再为租客启用 WhatsApp，随后点击失败记录的“重试”。
- 期望结果：重试时使用租客当前已授权的 WhatsApp 号码。
- 实际结果：重复失败并提示 `WhatsApp phone number is required`；再次立即执行建立 0 条提醒。

## 复现步骤
1. 在租客尚未启用 WhatsApp 时执行租金逾期规则。
2. 系统建立通知和一条目的地为空的失败投递记录。
3. 为租客启用 WhatsApp 并填写有效号码。
4. 点击原失败投递的“重试”，记录仍使用空目的地并再次失败。

## 根因分析
- 表象：失败记录重试时提示缺少 WhatsApp 手机号。
- 根因：重试 SQL 仅重置投递状态，没有根据通知关联的租金账单、租约和租客重新读取当前 WhatsApp 授权号码。
- “立即执行建立 0 条”属于正常的防重复机制，因为相同规则和账单的通知已经存在。

## 修复方案
- 修改 `backend/src/main/java/com/ccps/backend/mapper/AdminReminderMapper.java`。
- WhatsApp 重试时关联通知、租金账单、租约及租客授权表，重新写入当前有效目的地后再进入待发送状态。
- 仅允许已启用且存在授权时间的 WhatsApp 订阅参与重试。
- 已为现有 3 条失败演示记录补齐目的地，状态仍保持失败，避免自动发送。

## 单元测试
- 测试文件：`backend/src/test/java/com/ccps/backend/mapper/AdminReminderMapperSqlTest.java`
- 测试用例：`retryingWhatsAppDeliveryRefreshesDestinationFromCurrentTenantConsent`
- 修复前：失败。
- 修复后：相关 6 项测试全部通过。

## 手动验证步骤
1. 刷新自动提醒页面并进入“发送结果”。
2. 找到测试租客对应的失败 WhatsApp 记录。
3. 点击“重试”。
4. 确认目的地显示租客号码，且不再出现手机号为空错误。

## 影响范围
- 直接影响：租金逾期 WhatsApp 失败投递的重试。
- 间接影响：无；通知防重复逻辑保持不变。
- 回归测试结果：通过。
