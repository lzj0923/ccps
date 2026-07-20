# CCPS 房產管理系統

這是一個使用 Vue 3 + Vite 重構的房產管理前端。原本集中在 `index.html` 的頁面，已拆分成多個頁面、元件、資料模組與互動邏輯模組。

## 開發環境

- Node.js
- Vue 3
- Vite 6
- `@lucide/vue` 圖標庫

## 安裝與執行

```bash
cd frontend
npm install
npm run dev
```

開發伺服器啟動後，開啟終端機顯示的本機網址即可。

## 生產編譯

```bash
cd frontend
npm run build
```

編譯結果會輸出到 `frontend/dist/`。部署時提供 `frontend/dist/` 內的檔案即可。

## MySQL 資料庫設計

資料庫設計文件和 MySQL 8.0 DDL 位於 `database/`：

```text
database/
├─ DATABASE_DESIGN.md   # 頁面功能、資料表和關聯說明
└─ schema.sql           # 可執行的 MySQL 8.0+ 建表腳本
```

詳細說明請查看 [database/DATABASE_DESIGN.md](database/DATABASE_DESIGN.md)。

## 專案結構

```text
frontend/
├─ src/
│  ├─ components/            # 可重用的畫面元件
│  ├─ pages/                 # 頁面級模組
│  ├─ composables/           # 頁面狀態、計算模型與互動行為
│  ├─ data/                  # 建案、表格、KPI 與報表資料
│  ├─ utils/                 # 格式化工具
│  ├─ App.vue                # 全局頁面殼層與頁面分流
│  ├─ pageBridge.js          # 子元件存取頁面狀態的橋接介面
│  └─ main.js                # Vue 應用程式入口
├─ admin-sidebar-reference.png
├─ index.html
├─ styles.css
├─ vite.config.js
├─ package.json
└─ dist/                     # Vite 編譯輸出
```

## 重構原則

- `App.vue` 只負責全局殼層、頁面分流與共用彈窗/通知。
- `pages/` 負責組合各個頁面區塊，不把所有內容堆在單一 Vue 檔案。
- `components/` 放可重用的 UI 元件。
- `data/` 集中管理展示資料，避免資料散落在模板中。
- `composables/` 管理狀態、計算屬性、篩選、報表與操作行為。
- 所有來源檔案使用 UTF-8 編碼，避免中文亂碼。

## 靜態資源

目前頁面已移除不必要的 `assets/` 資料夾。管理後臺側邊欄使用 `frontend/admin-sidebar-reference.png`，透過 CSS 只顯示參考圖中的城市景觀區域；房產卡使用 CSS 純色標識，頂部區域使用純色漸變。

## 注意事項

- `frontend/dist/` 是編譯產物，不應直接修改。
- 修改 `frontend/src/` 或 `frontend/styles.css` 後，在 `frontend/` 目錄重新執行 `npm run build`。
- 頁面可以透過 URL 的 `page` 參數開啟指定模組，例如：

```text
http://localhost:5173/?page=myProperties
```
