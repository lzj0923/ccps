# 租约到期业务人员 WhatsApp 通知

## 功能

- `LEASE_EXPIRY_BUSINESS_30D` 是系统自动规则：后端启动完成后立即扫描，之后每小时自动执行。
- 系统会在每次启动时校正规则为提前 30 天、WhatsApp、业务人员及启用状态。
- 该系统规则不能人工编辑、停用、删除或点击“立即执行”。
- 租约进入结束前 30 天时建立一次通知。
- 收件人取租约关联租管委托的负责人；负责人为空时回退到委托建立人。
- WhatsApp 号码取员工账号的 `users.phone`，发送记录及结果显示在“自动提醒 → 投递结果”。
- 同一规则、租约及业务人员不会重复建立通知。

## Meta 模板

在 WhatsApp Manager 建立并审核 `UTILITY` 模板，默认名称：

`ccps_lease_expiry_business_notice`

建议正文：

`您好 {{1}}，{{2}} 的租约 {{3}} 将于 {{4}} 到期，请及时跟进续约或退租安排。`

变量依次为：业务人员姓名、建案及单位、租约编号、租约结束日期。

如使用其他模板名称，设置环境变量：

`WHATSAPP_TEMPLATE_LEASE_EXPIRY_BUSINESS`

## 上线步骤

1. 执行 `database/migrate_lease_expiry_business_whatsapp.sql`。
2. 在员工账号中为业务负责人填写可转成 E.164 格式的 WhatsApp 手机号。
3. 确保租约已关联租管委托，且委托已指定负责人或保留有效建立人。
4. 在 Meta 完成模板审核并配置现有 WhatsApp Graph API 环境变量。
5. 启动后端；系统会立即执行首次扫描，后续投递结果可在“自动提醒 → 投递结果”查看。
