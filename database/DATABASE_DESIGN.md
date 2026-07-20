# CCPS MySQL 資料庫設計

這份設計是根據目前 Vue 前端的實際頁面和互動整理出來的，對應 `frontend/src/data/dashboardData.js` 裡的業主端、管理後台、KPI、列表、提醒、報表和 SQL Account 同步資料。

## 1. 設計結論

前端目前是展示型資料，表格裡的數字和狀態仍是字串。正式資料庫需要把它們拆成可關聯、可篩選、可追蹤的真實欄位：金額使用 `DECIMAL(18,2)`，日期使用 `DATE/DATETIME`，狀態使用可演進的 `VARCHAR`，附件使用文件表，跨模組操作使用財務、通知、同步和審計記錄。

核心主資料是：

```text
users / roles
        │
projects ── units ── owner_units ── owners
                    │       │
                    │       ├─ purchase_contracts ── payment_plans ── payment_installments
                    │       │                                             │
                    │       │                              payment_receipts ── finance_records
                    │       │
                    │       ├─ reserve_accounts ── reserve_transactions
                    │       │
                    │       └─ leases ── tenants ── rent_invoices ── rent_payments
                    │
                    ├─ cashflow_entries ── maintenance_work_orders
                    └─ documents / notifications / reports / SQL sync
```

帳戶入口由 `users.account_type` 明確區分，只允許 `ADMIN`（管理端）與 `OWNER`（業主端）兩種互斥類型。`roles / user_roles` 繼續負責類型內的功能權限；它們不能覆蓋帳戶入口類型。業主帳戶再透過 `owners.user_id` 綁定業主與名下房產，管理員帳戶不得綁為業主登入身份。

可執行的完整 DDL 在 [schema.sql](./schema.sql)，按 MySQL 5.7+ 編寫，推薦使用 MySQL 8.0。MySQL 5.7 會解析但不強制執行 `CHECK` 條件，因此 5.7 環境仍要在後端做欄位驗證。

## 2. 前端功能與資料表對照

| 前端功能 | 主要資料表 | 支援的操作 |
| --- | --- | --- |
| 業主與房產 | `owners`, `projects`, `units`, `owner_units` | 業主建檔、綁定單位、查看房產總價與狀態 |
| 房款進度 | `purchase_contracts`, `payment_plans`, `payment_installments`, `payment_receipts` | 設定分期、計算已繳/未繳、到期提醒、上傳付款憑證 |
| 租客與租金 | `tenants`, `leases`, `rent_invoices`, `rent_payments` | 新增租客/租約、月度出帳、部分收款、逾期查詢 |
| 收支與維修 | `finance_records`, `cashflow_entries`, `maintenance_work_orders`, `vendors` | 租金收入、維修/水電/管理費、工單、供應商、支付狀態 |
| 財務確認 | `finance_records`, `payment_receipts`, `cashflow_entries` | 待確認、部分付款、已確認、退回補件、會計同步狀態 |
| 預備金 | `reserve_accounts`, `reserve_transactions` | 最低標準、充值、扣款、餘額、低餘額提醒 |
| 文件資料 | `documents`, `document_links` | 買賣合約、租約、付款憑證、維修附件、審核、到期日 |
| 通知中心/自動提醒 | `notification_rules`, `notifications`, `notification_deliveries` | 規則、站內通知、Email/LINE、已讀、失敗原因、重發 |
| SQL Account 同步 | `sync_batches`, `sync_batch_items`, `finance_records.sync_status` | 批次、成功/失敗筆數、錯誤原因、重試、同步日誌 |
| 報表與導出 | `report_definitions`, `report_runs` | 日期/項目/狀態篩選、PDF/XLSX、排程、下載、歸檔 |
| 權限與操作追蹤 | `users`, `roles`, `permissions`, `audit_logs` | 業主端/後台角色、功能權限、變更追蹤 |

## 3. 重要資料設計決策

### 3.1 不為每個頁面各建一張表

業主端和管理後台顯示的是同一批業務資料，只是查詢視角不同。例如：

- 業主端「房款進度」與後台「建築與房款」共用付款計畫與分期表。
- 業主端「租金收入」與後台「租客與租金」共用租約、租金帳單和收款表。
- 業主端「收支維修」與後台「收支與維修」共用財務記錄和收支明細。

這樣前端頁面只需要不同的查詢條件與權限，後端不會因為頁面增加而複製資料。

### 3.2 `finance_records` 是財務確認的共用接口

房款、租金、收支和預備金異動都可以進入財務確認與 SQL Account 同步流程，所以使用 `finance_records` 統一保存交易編號、金額、付款狀態、財務確認狀態和同步狀態。各業務表再用一對一或一對多關係保存自己的細節。

這個接口的好處是：財務頁面只需要查一個待確認隊列，報表和同步也可以共用相同的交易來源。

### 3.3 文件採用「文件本體 + 關聯」

`documents` 只保存文件名稱、儲存鍵、格式、審核和到期日；`document_links` 保存它屬於哪個業務對象。這能同時支援買賣合約、付款憑證、租約和維修附件。

`document_links.entity_id` 是跨模組關聯欄位，MySQL 無法對多種目標表建立單一外鍵，因此後端寫入時必須做類型與 ID 驗證，並在刪除業務資料時同步清理關聯。

### 3.4 狀態不要用展示文字當資料值

目前 UI 顯示的是「待確認」「已完成」「同步失敗」等中文。正式 API 建議使用穩定的英文 code，再由前端做翻譯，例如：

| UI 顯示 | 建議 code |
| --- | --- |
| 待確認 | `pending` |
| 部分付款 | `partial` |
| 已付款 | `paid` |
| 逾期 | `overdue` |
| 同步中 | `processing` |
| 同步失敗 | `failed` |

這樣不會因為改語言或改文案而破壞查詢和統計。

### 3.5 支出優先使用預備金

已確認實際金額的支出會先查找該業主單位的有效預備金帳戶：餘額足夠時建立 `reserve_transactions` 扣款、把 `finance_records.payment_method` 設為 `reserve_account`，並將 `cashflow_entries.attachment_status` 設為 `not_required`；餘額不足時不建立扣款，維持待付款狀態並要求付款憑證。這個規則由 `migrate_reserve_expense_policy.sql` 的資料庫觸發器和既有資料回補程序共同保證，扣款時必須在交易中鎖定預備金餘額。

## 4. Dashboard 查詢接口

DDL 內附了 5 個 read model view，供後端 API 或報表模組直接使用：

- `v_unit_payment_progress`：單位房款總價、已繳、未繳和進度。
- `v_rent_collection_status`：月租帳單、已收、未收、部分收款和逾期狀態。
- `v_reserve_balances`：預備金餘額、最低標準和不足金額。
- `v_finance_review_queue`：財務待確認/未付款處理隊列。
- `v_sync_batch_summary`：SQL Account 批次成功率和失敗數。

前端目前的 `currentMetrics`、列表篩選、報表統計應由後端 API 基於這些 read model 返回，不應再依靠把整行字串讀出來判斷金額或狀態。

## 5. 建議的後端接口分組

資料庫完成後，前端可以按下面的深模組接口接 API。每個接口隱藏 SQL、權限、分頁和狀態轉換，頁面只接收穩定的資料結構：

```text
PropertyModule
  listProjects(), listUnits(), listOwners(), saveOwner(), saveUnit()

PaymentModule
  getProgress(unitId), listInstallments(), uploadReceipt(), reviewReceipt()

TenancyModule
  listLeases(), saveLease(), listRentInvoices(), recordRentPayment()

FinanceModule
  listReviewQueue(), confirmTransaction(), rejectTransaction(), exportCashflow()

ReserveModule
  getBalance(ownerUnitId), topUp(), debit(), updateMinimumBalance()

NotificationModule
  listNotifications(), markRead(), saveRule(), resendDelivery()

SyncModule
  createBatch(), retryFailedItems(), getBatchDetail()

ReportModule
  createRun(), getRunStatus(), downloadRun()
```

這些是資料模組的接口，不是要求現在就把所有 API 一次寫完。先把 `dashboardData.js` 替換成一個 API adapter，再逐頁接上，能保留目前頁面結構並集中處理錯誤、權限和 loading 狀態。

## 6. 上線前必做事項

1. 將 `owners.identity_no`、`tenants.identity_no` 和文件儲存鍵視為敏感資料，做加密、存取權限和下載審計。
2. 所有金額運算使用 `DECIMAL`，不要在 API 或 MySQL 中用浮點數。
3. 寫入付款、財務確認、預備金扣款時使用交易（transaction），並以鎖定或版本欄位避免餘額競態。
4. 以後端權限控制業主只能看自己的 `owner_units`，後台角色才可看跨業主資料。
5. `document_links`、同步批次和通知發送都要保留審計與失敗重試資訊。
6. 先做 migration 工具，再導入 `dashboardData.js` 的展示資料；不要把展示字串直接灌進正式欄位。
