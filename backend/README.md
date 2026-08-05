# CCPS Backend

CCPS 房產管理系統的 Spring Boot 後端分層專案。

## 環境需求

- Java 17+
- Maven 3.9+
- MySQL 8+

## 資料庫設定

預設連接本機 MySQL：`localhost:3306/ccps_property_management`，帳號 `root`，密碼 `123456`。
正式環境請使用環境變數覆寫：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。
MyBatis-Plus 負責資料庫增刪改查，核心模組的生成代碼位於 `com.ccps.backend.generated`。

## 啟動

```bash
cd backend
mvn spring-boot:run
```

啟動後可用以下網址確認服務正常：

```text
http://localhost:8080/api/health
```

## 專案結構

```text
backend/
├─ src/main/java/com/ccps/backend/
│  ├─ config/       # Web、CORS 等設定
│  ├─ controller/   # REST API
│  ├─ service/      # 業務邏輯
│  ├─ mapper/       # MyBatis-Plus Mapper / BaseMapper
│  ├─ model/        # 領域模型與列舉
│  ├─ dto/          # API 請求/回應模型
│  └─ exception/    # 統一例外處理
└─ src/main/resources/application.yml
```

## 目前 API

- `GET /api/owner/dashboard`：依登入使用者查詢「我的房產」頁面資料（名下單位、房款、租金、預備金、通知與待辦）
- `GET /api/owner/rent-income`：依登入業主查詢租金收入 KPI、年度趨勢、收款記錄與最近到帳（支援年份、月份、房產及狀態篩選）
- `GET /api/owner/expenses`：依登入業主查詢支出、維修、預備金扣款 KPI 及篩選選項
- `GET /api/owner/expenses/maintenance/{workOrderId}`：查詢維修單、五階段進度、財務狀態與附件
- `POST /api/owner/expenses/maintenance/{workOrderId}/attachments`：上傳維修前照片、維修後照片或發票（`relationType` 為 `before_photo`、`after_photo`、`invoice`）
- `GET /api/owner/expenses/attachments/{documentId}`：驗證業主權限後預覽附件；加上 `?download=true` 可下載
- `GET /api/owner/reserve`：查詢登入業主的預備金摘要、賬戶、交易、通知及充值憑證（支援房產、日期及交易類型篩選）
- `POST /api/owner/reserve/topups`：提交預備金充值及付款憑證；申請先進入待審核狀態，不會直接改動餘額
- `GET /api/owner/reserve/documents/{documentId}`：驗證業主權限後預覽充值憑證；加上 `?download=true` 可下載
- `POST /api/admin/reserve/topups/{financeRecordId}/review`：僅限管理員批准或拒絕充值；批准後以資料庫交易同步新增流水及更新餘額
- `GET /api/owner/notifications`：查詢通知統計、分類、通知列表、待處理事項及接收方式
- `POST /api/owner/notifications/{notificationId}/read`：將指定通知標記為已讀（會驗證業主權限）
- `POST /api/owner/notifications/read-all`：將當前業主的全部通知標記為已讀
- `GET /api/admin/reminders`：查詢管理端提醒規則、通知記錄、管道發送結果及統計
- `POST /api/admin/reminders/rules`：新增房款、租金、租約、預備金或文件到期提醒規則
- `PUT /api/admin/reminders/rules/{ruleId}`：修改提醒規則；`/enabled?enabled=true|false` 可啟用或停用
- `POST /api/admin/reminders/rules/{ruleId}/run`：立即執行指定規則；`POST /api/admin/reminders/run` 執行全部已啟用規則
- `POST /api/admin/reminders/deliveries/{deliveryId}/retry`：將失敗的 Email 發送重新加入處理佇列
- `GET /api/admin/sync`：查詢 SQL Account 匯入檔批次、明細及同步統計
- `POST /api/admin/sync/preview`：依資料類型、日期及建案預覽可匯出的已確認財務資料
- `POST /api/admin/sync/batches`：建立同步批次並產出標準 CSV 匯入檔；`/batches/{id}/download` 可下載，`/retry` 可重試失敗批次
- `GET /api/admin/reports`：查詢報表定義、執行歷史、建案選項及統計
- `POST /api/admin/reports/runs`：依報表類型、日期、建案及格式產出 XLSX 或 PDF；`/runs/{id}/download` 可下載
- `GET /api/owner/documents`：依登入業主查詢文件總數、分類統計、文件列表及所屬房產
- `GET /api/owner/documents/{documentId}/file`：驗證業主權限後預覽文件；加上 `?download=true` 可下載
- `GET /api/properties`：查詢全部物業
- `GET /api/properties/{id}`：查詢單筆物業
- `POST /api/properties`：新增物業
- `PUT /api/properties/{id}`：更新物業
- `DELETE /api/properties/{id}`：刪除物業

`POST`/`PUT` 的 JSON 欄位：`name`、`projectName`、`address`、`price`、`area`、`bedrooms`、`status`。狀態可用 `AVAILABLE`、`RESERVED`、`SOLD`、`OFFLINE`。

既有資料庫升級支出維修模組前，先執行 `database/migrate_expense_maintenance.sql`。附件預設存放於 `uploads/maintenance-attachments`，正式環境可用 `MAINTENANCE_ATTACHMENT_STORAGE` 指定持久化目錄。

既有資料庫升級預備金充值模組前，先執行 `database/migrate_reserve.sql`。充值憑證預設存放於 `uploads/reserve-topups`，正式環境可用 `RESERVE_TOPUP_STORAGE` 指定持久化目錄。
既有資料庫升級租客押金財務確認模組前，先執行 `database/migrate_security_deposits.sql`。

文件資料模塊直接使用 `documents` 與 `document_links` 表，文件檔案預設從 `uploads` 及其 `payment-proofs`、`maintenance-attachments`、`reserve-topups`、`documents` 子目錄讀取；正式環境可用 `DOCUMENT_STORAGE_ROOT` 指定檔案根目錄。文件列表與下載 API 會依登入業主的 owner、unit、finance、work_order 關聯做權限篩選。

自動提醒直接使用 `notification_rules`、`notifications`、`notification_deliveries` 與 `notification_subscriptions` 表。後端預設每小時執行一次已啟用規則，可用 `REMINDER_CRON` 覆寫 Spring cron；Email 仍需設定 `MAIL_HOST`、`MAIL_USERNAME`、`MAIL_PASSWORD`、`MAIL_FROM`，並由業主完成郵箱驗證。

SQL Account 目前採用可替換的 `AccountingSyncGateway`。預設 Gateway 會把已確認且尚未同步的財務記錄產出為標準 CSV 匯入檔，存放於 `uploads/accounting-exports`，可用 `ACCOUNTING_EXPORT_STORAGE` 覆寫。這個階段只會把記錄標為 `exported`；取得目標 SQL Account 的 API、SDK、資料庫或正式匯入規格後，再新增 Gateway 並以外部回執決定 `synced` 狀態。

報表直接讀取 CCPS 資料庫，檔案預設存放於 `uploads/reports`，可用 `REPORT_STORAGE_ROOT` 覆寫。支援 XLSX 與 PDF，報表執行狀態、條件、記錄數、檔案位置及錯誤均保留於 `report_runs`。
