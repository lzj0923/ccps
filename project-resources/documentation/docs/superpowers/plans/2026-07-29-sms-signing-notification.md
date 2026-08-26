# 短信線上簽約通知 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在現有電子簽署流程中加入馬來西亞手機短信通知，讓業主授權委託書、租賃合約等簽署文件都能發送簽署連結及一次性驗證碼。

**Architecture:** 保留現有電子簽署請求及公開簽署頁，新增 `SmsGateway` 介面隔離短信服務商；簽署請求以 `email` 或 `sms` 作為通知渠道，短信發送及重發寫入獨立投遞記錄。前端使用同一個簽署邀請視窗，依文件和簽署角色自動帶出業主／租客手機號碼。

**Tech Stack:** Spring Boot 3.4.5、MyBatis、MySQL、Vue 3、現有 `propertyApi.js` API 層、現有 i18n 三語系。

## Global Constraints

- 簡訊只發送簽署連結、一次性驗證碼、文件名稱及有效期限，不發送身分證號、銀行資料或完整合約。
- 手機號碼優先讀取業主／租客資料；資料缺失時才允許手動輸入。
- 驗證碼有效 10 分鐘、最多輸入 5 次、重發間隔 60 秒；重發會使舊碼失效。
- 簽署連結使用一次性隨機令牌；文件已簽署或出租委託已啟用時禁止重新簽署。
- 新增文字必須同時提供簡體中文、繁體中文及英文。
- 正式環境的短信服務商設定只能透過環境變數注入，不能把 API 金鑰寫入程式碼或資料庫。

---

### Task 1: 擴充簽署請求與短信投遞資料表

**Files:**
- Create: `database/migrate_sms_signature_notifications.sql`
- Modify: `database/schema.sql`（同步新增欄位與資料表定義）
- Modify: `backend/src/main/java/com/ccps/backend/dto/ElectronicSignatureStartRequest.java`
- Create: `backend/src/main/java/com/ccps/backend/dto/ElectronicSignatureNotificationResponse.java`
- Test: `backend/src/test/java/com/ccps/backend/mapper/ElectronicSignatureMapperSqlTest.java`

**Interfaces:**
- `ElectronicSignatureStartRequest` 新增 `signerPhone`、`channel`；`channel` 只接受 `email` 或 `sms`。`signerEmail` 改為可空，由服務層依渠道驗證。
- `ElectronicSignatureNotificationResponse` 提供 `requestId`、`channel`、`maskedPhone`、`status`、`sentAt`、`resendAvailableAt`、`failureReason`。

- [ ] **Step 1: 寫資料庫失敗測試**

在 mapper SQL 測試中讀取 migration 內容，要求包含 `signer_phone`、`notification_channel`、短信投遞狀態及重發冷卻欄位，並要求短信記錄以簽署請求 ID 建立索引。

- [ ] **Step 2: 執行測試確認先失敗**

Run: `node --test backend/src/test/java/com/ccps/backend/mapper/ElectronicSignatureMapperSqlTest.java`

Expected: FAIL，因為 migration 尚未包含短信欄位。

- [ ] **Step 3: 建立 migration**

在 `electronic_signature_requests` 增加：

```sql
ALTER TABLE electronic_signature_requests
  ADD COLUMN signer_phone VARCHAR(40) NULL AFTER signer_email,
  ADD COLUMN notification_channel VARCHAR(20) NOT NULL DEFAULT 'email' AFTER signer_phone;

CREATE TABLE IF NOT EXISTS electronic_signature_sms_deliveries (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  signature_request_id BIGINT UNSIGNED NOT NULL,
  phone_e164 VARCHAR(24) NOT NULL,
  message_id VARCHAR(190) NULL,
  status VARCHAR(20) NOT NULL,
  attempt_no INT NOT NULL DEFAULT 1,
  failure_reason VARCHAR(500) NULL,
  sent_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sms_delivery_request (signature_request_id, created_at),
  CONSTRAINT fk_sms_delivery_request FOREIGN KEY (signature_request_id)
    REFERENCES electronic_signature_requests (id)
) ENGINE=InnoDB;
```

`status` 使用 `queued`、`sent`、`failed`、`delivered` 四種值；migration 要可重複執行，使用專案現有的欄位存在檢查方式。

- [ ] **Step 4: 更新啟動請求 DTO 與通知回應 DTO**

移除 DTO 對 email 的強制註解，保留姓名必填；在服務層依渠道要求 email 或 phone。新增回應 record，供管理端查看發送狀態。

- [ ] **Step 5: 重新執行 mapper SQL 測試**

Run: `node --test backend/src/test/java/com/ccps/backend/mapper/ElectronicSignatureMapperSqlTest.java`

Expected: PASS。

- [ ] **Step 6: Commit**

```bash
git add database/migrate_sms_signature_notifications.sql database/schema.sql backend/src/main/java/com/ccps/backend/dto/ElectronicSignatureStartRequest.java backend/src/main/java/com/ccps/backend/dto/ElectronicSignatureNotificationResponse.java backend/src/test/java/com/ccps/backend/mapper/ElectronicSignatureMapperSqlTest.java
git commit -m "feat: add sms signature notification schema"
```

### Task 2: 建立短信服務商適配層

**Files:**
- Create: `backend/src/main/java/com/ccps/backend/service/sms/SmsGateway.java`
- Create: `backend/src/main/java/com/ccps/backend/service/sms/SmsMessage.java`
- Create: `backend/src/main/java/com/ccps/backend/service/sms/SmsSendResult.java`
- Create: `backend/src/main/java/com/ccps/backend/service/sms/HttpSmsGateway.java`
- Create: `backend/src/main/java/com/ccps/backend/service/sms/PhoneNumberNormalizer.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/ccps/backend/service/SmsGatewayTest.java`

**Interfaces:**
- `SmsGateway.send(SmsMessage message)` 回傳 `SmsSendResult(status, providerMessageId, failureReason)`。
- `SmsMessage` 包含 E.164 電話、短信正文、簽署請求 ID。
- `PhoneNumberNormalizer.normalizeMalaysia(String raw)` 回傳 `+60...`；無法辨識時拋出可顯示的驗證錯誤。

- [ ] **Step 1: 寫電話格式與服務停用測試**

測試 `012-3456789`、`+60123456789` 都轉成同一個 E.164 值；字母、空號、錯誤國碼必須失敗。當 `SMS_ENABLED=false` 時，gateway 必須返回明確的未設定錯誤，不可假裝已送出。

- [ ] **Step 2: 執行測試確認先失敗**

Run: `mvn -q -Dtest=SmsGatewayTest test`

Expected: FAIL，因為短信 gateway 尚未建立。

- [ ] **Step 3: 實作適配層**

`HttpSmsGateway` 只負責呼叫供應商 HTTP API，不知道電子簽署業務。設定項目使用：

```yaml
ccps:
  sms:
    enabled: ${SMS_ENABLED:false}
    endpoint: ${SMS_ENDPOINT:}
    api-key: ${SMS_API_KEY:}
    sender-id: ${SMS_SENDER_ID:}
    timeout-ms: ${SMS_TIMEOUT_MS:8000}
```

供應商回傳非 2xx、超時或缺少訊息 ID 時都回傳 `failed`，不得回傳 `sent`。

- [ ] **Step 4: 通過 gateway 測試**

Run: `mvn -q -Dtest=SmsGatewayTest test`

Expected: PASS，並確認不會在測試中發送真短信。

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ccps/backend/service/sms backend/src/main/resources/application.yml backend/src/test/java/com/ccps/backend/service/SmsGatewayTest.java
git commit -m "feat: add malaysia sms gateway adapter"
```

### Task 3: 把短信渠道接入電子簽署服務

**Files:**
- Modify: `backend/src/main/java/com/ccps/backend/mapper/ElectronicSignatureMapper.java`
- Modify: `backend/src/main/java/com/ccps/backend/service/ElectronicSignatureService.java`
- Modify: `backend/src/main/java/com/ccps/backend/controller/AdminElectronicSignatureController.java`
- Modify: `backend/src/main/java/com/ccps/backend/dto/ElectronicSignatureStartResponse.java`
- Test: `backend/src/test/java/com/ccps/backend/service/ElectronicSignatureServiceTest.java`

**Interfaces:**
- 現有 `/api/admin/e-signatures/leases/{leaseId}` 及 `/rental-mandates/{mandateId}/documents/{documentId}` 保持不變；request 的 `channel=sms` 走短信，`channel=email` 維持原電郵流程。
- 新增 `GET /api/admin/e-signatures/{requestId}/notification` 查詢發送狀態。
- 新增 `POST /api/admin/e-signatures/{requestId}/resend`，由管理端重發最新驗證碼；服務層強制 60 秒冷卻及邀請狀態檢查。

- [ ] **Step 1: 寫短信渠道失敗測試**

使用 fake `SmsGateway` 測試：短信渠道沒有 phone 時失敗；有效 phone 會保存 `notification_channel=sms`、投遞 `sent` 記錄及審計事件；gateway 失敗時簽署請求不標記為已送出；重發冷卻及已簽署／已啟用委托會被拒絕。

- [ ] **Step 2: 執行測試確認先失敗**

Run: `mvn -q -Dtest=ElectronicSignatureServiceTest test`

Expected: FAIL，因為現有服務只會寄送 email。

- [ ] **Step 3: 實作渠道分流**

在 `ElectronicSignatureService.start(...)` 中：

```java
String channel = request.channel() == null ? "email" : request.channel().trim().toLowerCase(Locale.ROOT);
if ("sms".equals(channel)) {
    String phone = PhoneNumberNormalizer.normalizeMalaysia(request.signerPhone());
    // 保存 phone 與 channel，產生驗證碼後交給 SmsGateway。
} else {
    String email = trim(request.signerEmail(), "Signer email").toLowerCase(Locale.ROOT);
    // 保留現有 JavaMailSender 流程。
}
```

只在短信服務商成功回傳後寫入 `sent` 及 `requested` 事件；失敗時保存 `failed` 和原因並向前端返回可讀錯誤。

- [ ] **Step 4: 實作查詢與重發接口**

mapper 增加最新投遞記錄查詢、發送次數及最後發送時間查詢；controller 只允許管理員調用。重發重新產生驗證碼、更新雜湊及有效期，並使前一個驗證碼失效。

- [ ] **Step 5: 執行後端測試**

Run: `mvn -q -Dtest=ElectronicSignatureServiceTest test`

Expected: PASS。

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/ccps/backend/mapper/ElectronicSignatureMapper.java backend/src/main/java/com/ccps/backend/service/ElectronicSignatureService.java backend/src/main/java/com/ccps/backend/controller/AdminElectronicSignatureController.java backend/src/main/java/com/ccps/backend/dto/ElectronicSignatureStartResponse.java backend/src/test/java/com/ccps/backend/service/ElectronicSignatureServiceTest.java
git commit -m "feat: send electronic signing invitations by sms"
```

### Task 4: 擴充前端簽署邀請視窗

**Files:**
- Create: `frontend/src/components/SignatureInviteDialog.vue`
- Modify: `frontend/src/components/AdminRentalMandateWorkspace.vue`
- Modify: `frontend/src/components/AdminTenancyWorkspace.vue`
- Modify: `frontend/src/services/propertyApi.js`
- Modify: `frontend/src/i18n/index.js`
- Test: `frontend/tests/electronic-signature-sms-invite.test.mjs`

**Interfaces:**
- `SignatureInviteDialog` props：`signerName`、`email`、`phone`、`documentName`；emit：`submit(payload)`、`close`。
- `payload` 形狀固定為 `{ signerName, signerEmail, signerPhone, channel, expiresInDays }`。
- `propertyApi.js` 新增 `fetchAdminSignatureNotification(requestId)`、`resendAdminSignatureCode(requestId)`。

- [ ] **Step 1: 寫前端失敗測試**

測試必須確認：選擇短信時自動顯示遮罩電話及驗證提示；有 owner／tenant phone 時不出現手動電話要求；已啟用委托或已簽署文件沒有短信簽署入口；發送失敗會顯示錯誤而不是成功 toast。

- [ ] **Step 2: 執行測試確認先失敗**

Run: `node --test frontend/tests/electronic-signature-sms-invite.test.mjs`

Expected: FAIL，因為目前只有 email 欄位及 `window.prompt` 流程。

- [ ] **Step 3: 建立共用邀請視窗**

視窗提供「電郵／短信」渠道選擇；短信渠道預填手機號碼並只顯示後四位給確認。手機號碼缺失才允許輸入；提交前驗證馬來西亞號碼格式。顯示有效天數及「發送簽署邀請」按鈕。

- [ ] **Step 4: 接入出租委托及租賃合約**

把 `AdminRentalMandateWorkspace.vue` 和 `AdminTenancyWorkspace.vue` 現有 prompt 改為打開共用視窗；業主使用 `ownerPhone`，租客使用 `tenantPhone`；成功後刷新附件／租約簽署狀態。

- [ ] **Step 5: 新增發送狀態與重發操作**

成功後顯示 `sent`；服務商失敗顯示原因與「重新發送」按鈕；重發按鈕依 `resendAvailableAt` 倒數，未到時間保持停用。

- [ ] **Step 6: 補齊三語 i18n 並執行測試**

Run: `node --test frontend/tests/electronic-signature-sms-invite.test.mjs`

Expected: PASS。

- [ ] **Step 7: Commit**

```bash
git add frontend/src/components/SignatureInviteDialog.vue frontend/src/components/AdminRentalMandateWorkspace.vue frontend/src/components/AdminTenancyWorkspace.vue frontend/src/services/propertyApi.js frontend/src/i18n/index.js frontend/tests/electronic-signature-sms-invite.test.mjs
git commit -m "feat: add sms signing invite dialog"
```

### Task 5: 公開簽署頁與驗證碼狀態提示

**Files:**
- Modify: `backend/src/main/java/com/ccps/backend/dto/ElectronicSignaturePublicResponse.java`
- Modify: `backend/src/main/java/com/ccps/backend/service/ElectronicSignatureService.java`
- Modify: `frontend/src/pages/SignaturePage.vue`
- Modify: `frontend/src/i18n/index.js`
- Test: `frontend/tests/signature-page-sms-copy.test.mjs`

**Interfaces:**
- 公開回應新增 `notificationChannel`、`verificationExpiresAt`、`resendAvailableAt`、`signerPhoneMasked`，不返回明文電話、驗證碼或 email。

- [ ] **Step 1: 寫公開頁失敗測試**

測試短信渠道顯示「驗證碼已發送至 +60****6789」；過期顯示重新發送提示；公開回應不包含明文電話或驗證碼。

- [ ] **Step 2: 更新公開回應及頁面**

`SignaturePage.vue` 根據 `notificationChannel` 顯示電郵或短信文案；短信重發成功顯示「新的驗證碼已發送至手機」，不再顯示目前的「已寄出電郵」文字。

- [ ] **Step 3: 執行測試**

Run: `node --test frontend/tests/signature-page-sms-copy.test.mjs`

Expected: PASS。

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ccps/backend/dto/ElectronicSignaturePublicResponse.java backend/src/main/java/com/ccps/backend/service/ElectronicSignatureService.java frontend/src/pages/SignaturePage.vue frontend/src/i18n/index.js frontend/tests/signature-page-sms-copy.test.mjs
git commit -m "feat: show sms verification state on signing page"
```

### Task 6: 整合測試、設定文件與上線檢查

**Files:**
- Create: `backend/src/test/java/com/ccps/backend/controller/ElectronicSignatureSmsControllerTest.java`
- Create: `project-resources/documentation/docs/短信簽約設定說明.md`
- Modify: `project-resources/documentation/docs/客戶問題每日紀錄.md`

- [ ] **Step 1: 建立不發真短信的整合測試**

以 fake gateway 啟動 Spring context，驗證建立短信簽署、查詢狀態、重發冷卻、驗證失敗、成功簽署及啟用委托阻止流程；確認資料庫事件和審計記錄都有寫入。

- [ ] **Step 2: 執行完整前端回歸測試**

Run: `node --test frontend/tests/*.test.mjs`

Expected: 所有測試通過，且沒有 i18n 缺鍵或白屏錯誤。

- [ ] **Step 3: 執行後端測試**

Run: `mvn -q test`

Expected: 退出碼 0；若環境沒有 Maven，必須在服務器的後端目錄執行並保留輸出，不以未執行當成通過。

- [ ] **Step 4: 撰寫部署設定說明**

`project-resources/documentation/docs/短信簽約設定說明.md` 要列出 `SMS_ENABLED`、`SMS_ENDPOINT`、`SMS_API_KEY`、`SMS_SENDER_ID`、`SMS_TIMEOUT_MS`、資料庫 migration 執行順序，以及測試模式不會發真短信的說明。

- [ ] **Step 5: 更新每日紀錄並交付**

記錄短信簽約已加入、驗證碼安全限制、短信服務商設定及測試結果；同時提醒部署後重啟 Spring Boot 並使用一個測試手機號完成端到端驗證。

- [ ] **Step 6: Commit**

```bash
git add backend/src/test/java/com/ccps/backend/controller/ElectronicSignatureSmsControllerTest.java project-resources/documentation/docs/短信簽約設定說明.md project-resources/documentation/docs/客戶問題每日紀錄.md
git commit -m "test: verify sms signing flow and deployment settings"
```
