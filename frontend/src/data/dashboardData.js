import { moneyText } from '../utils/dashboardFormatters';

const detailBlocks = (a, b) => [
  { title: a.title, items: a.items.map(item => ({ label: item[0], value: item[1] })) },
  { title: b.title, items: b.items.map(item => ({ label: item[0], value: item[1] })) }
];

const modules = [
  {
    id: "ownerProjects", shell: "owner-shell", code: "00", name: "最新建案", mobileOnly: true, avatar: false,
    title: "最新建案", category: "LATEST PROJECTS", hero: "精选房产新项目",
    hint: "查看开放项目、项目资料与讲座信息。", table: "开放建案", searchHint: "搜索建案"
  },
  {
    id: "myProperties", shell: "owner-shell", code: "01", name: "我的房產", avatar: false,
    title: "我的房產", category: "OWNER PORTAL", hero: "業主端房產總覽",
    hint: "查看名下物業、付款摘要、租金狀態、預備金和文件提醒。",
    table: "名下房產列表", searchHint: "搜尋建案、單位、房產狀態",
    primaryAction: "查看房產", secondaryAction: "下載摘要",
    flowTitle: "業主查看流程", flowHint: "從房產總覽進入房款、租金、收支和文件。",
    quickActions: ["查看詳情", "房款進度", "租金收入", "下載文件"],
    cards: [
      { label: "Pavilion Square", title: "A-28-05", text: "2 Bedroom Residence", value: "正常" },
      { label: "CCP Residence", title: "B-12-07", text: "Studio Suite", value: "待確認" },
      { label: "CCPS Heights", title: "A-19-09", text: "Investment Unit", value: "出租中" }
    ],
    detailBlocks: detailBlocks(
      { title: "房產資訊", items: [["業主", "Tan Wei Ming"], ["建案", "Pavilion Square"], ["單位", "A-28-05"]] },
      { title: "狀態摘要", items: [["房款", "待確認"], ["租金", "正常"], ["文件", "已完成"]] }
    )
  },
  {
    id: "ownerPayment", shell: "owner-shell", code: "02", name: "房款進度", avatar: false,
    title: "房款進度詳情", category: "PAYMENT PROGRESS", hero: "業主端房款進度查詢",
    hint: "查看房產總價、總期數、已繳期數、下期到期日和每期付款狀態。",
    table: "分期付款明細", searchHint: "搜尋單位、期數、付款狀態",
    primaryAction: "上傳付款憑證", secondaryAction: "下載付款表",
    flowTitle: "房款進度流程", flowHint: "總價、期數、憑證與財務確認都清楚呈現。",
    quickActions: ["上傳憑證", "查看期數", "下載明細", "聯絡財務"],
    cards: [
      { label: "房產總價", title: "RM 1,300,000.00", text: "Pavilion Square A-28-05", value: "12 期" },
      { label: "已繳金額", title: "RM 1,200,000.00", text: "已確認 11 期", value: "92%" },
      { label: "未繳金額", title: "RM 100,000.00", text: "下期 2025-06-15", value: "待確認" }
    ],
    detailBlocks: detailBlocks(
      { title: "分期資料", items: [["總期數", "12 期"], ["已繳期數", "11 期"], ["下期到期", "2025-06-15"]] },
      { title: "付款狀態", items: [["付款憑證", "已完成"], ["財務確認", "待確認"], ["逾期提醒", "啟用"]] }
    )
  },
  {
    id: "ownerRentalHub", shell: "owner-shell", code: "03.0", name: "租管服务", mobileOnly: true, avatar: false,
    title: "租管服务", category: "RENTAL MANAGEMENT", hero: "租赁与房产收支",
    hint: "查看租金、租客、收支、预备金与租赁合同。", table: "租管资产", searchHint: "搜索房产或租客"
  },
  {
    id: "ownerFinance", shell: "owner-shell", code: "03", name: "財務中心", avatar: false,
    title: "財務中心", category: "OWNER FINANCE", hero: "業主端財務總覽",
    hint: "集中查看本月收入、支出、結餘、預備金與最近金額流水。",
    table: "最近金額流水", searchHint: "搜尋房產、說明或金額",
    primaryAction: "查看租金收入", secondaryAction: "查看收支維修",
    flowTitle: "業主財務中心", flowHint: "租金收入、收支維修和預備金集中管理。",
    quickActions: ["租金收入", "收支維修", "預備金"]
  },
  {
    id: "rentIncome", shell: "owner-shell", code: "03.1", name: "租金收入", avatar: true,
    title: "租金收入", category: "RENTAL INCOME", hero: "業主端租金收入查詢",
    hint: "全面掌握租金收入情況，輕鬆管理您的資產收益。",
    table: "租金收入列表", searchHint: "搜尋租客、月份、收款狀態",
    primaryAction: "查看租約", secondaryAction: "下載租金表",
    flowTitle: "租金收入流程", flowHint: "租客付款後進入財務確認並同步到業主端。",
    quickActions: ["查看租客", "租金歷史", "下載收據", "發送提醒"],
    cards: [
      { label: "本月租金", title: "RM 12,850.00", text: "3 個出租單位", value: "已收 82%" },
      { label: "待收租金", title: "RM 5,200.00", text: "Rachel Lee", value: "待確認" },
      { label: "租約到期", title: "90 天內 1 份", text: "A-19-09", value: "提醒中" }
    ],
    detailBlocks: detailBlocks(
      { title: "租金摘要", items: [["本月收入", "RM 12,850"], ["待收", "RM 5,200"], ["租客狀態", "正常"]] },
      { title: "租約文件", items: [["租約", "已完成"], ["收據", "已完成"], ["到期提醒", "啟用"]] }
    )
  },
  {
    id: "ownerExpenses", shell: "owner-shell", code: "03.2", name: "收支維修", avatar: false,
    title: "收支維修", category: "INCOME AND EXPENSE", hero: "業主端收入支出查詢",
    hint: "查看您的房產收支明細與維修記錄，掌握財務與維修狀態。",
    table: "收入與支出明細", searchHint: "搜尋支出、憑證、單位",
    primaryAction: "查看憑證", secondaryAction: "下載明細",
    flowTitle: "收支透明流程", flowHint: "支出明細和憑證同步顯示給業主查看。",
    quickActions: ["查看憑證", "下載明細", "提出疑問", "聯絡物業"],
    cards: [
      { label: "租金收入", title: "RM 12,850.00", text: "本月已確認", value: "正常" },
      { label: "維修費用", title: "RM 1,500.00", text: "2 筆維修", value: "待確認" },
      { label: "淨收入", title: "RM 11,350.00", text: "扣除支出後", value: "已完成" }
    ],
    detailBlocks: detailBlocks(
      { title: "收支資料", items: [["租金收入", "RM 12,850"], ["維修費", "RM 1,500"], ["管理費", "RM 380"]] },
      { title: "文件狀態", items: [["支出憑證", "已完成"], ["維修附件", "已完成"], ["財務確認", "待確認"]] }
    )
  },
  {
    id: "ownerReserve", shell: "owner-shell", code: "03.3", name: "預備金", avatar: false,
    title: "預備金與通知", category: "RESERVE AND NOTICE", hero: "業主端預備金與提醒中心",
    hint: "實時查看預備金餘額與交易明細，獲取最新通知與重要文件。",
    table: "預備金與通知紀錄", searchHint: "搜尋充值、扣款、通知",
    primaryAction: "補繳預備金", secondaryAction: "查看扣款",
    flowTitle: "預備金流程", flowHint: "維修扣款、充值、餘額和不足通知形成閉環。",
    quickActions: ["充值記錄", "扣款記錄", "補繳通知", "下載紀錄"],
    cards: [
      { label: "當前餘額", title: "RM 25,000.00", text: "最低標準 RM 20,000", value: "正常" },
      { label: "本月扣款", title: "RM 580.00", text: "冷氣維修", value: "已完成" },
      { label: "提醒", title: "0 則不足提醒", text: "餘額高於最低標準", value: "啟用" }
    ],
    detailBlocks: detailBlocks(
      { title: "預備金", items: [["目前餘額", "RM 25,000"], ["最低標準", "RM 20,000"], ["不足提醒", "啟用"]] },
      { title: "通知", items: [["充值通知", "啟用"], ["扣款通知", "啟用"], ["低餘額", "啟用"]] }
    )
  },
  {
    id: "ownerNotice", shell: "owner-shell", code: "06", name: "通知中心", avatar: false,
    title: "通知中心", category: "NOTIFICATION CENTER", hero: "業主端通知中心",
    hint: "重要通知及時掌握，事務處理更高效。",
    table: "通知列表", searchHint: "搜尋通知、類型、狀態",
    primaryAction: "全部已讀", secondaryAction: "通知設定",
    flowTitle: "通知處理流程", flowHint: "通知生成、發送、已讀、重發和失敗原因可追蹤。",
    quickActions: ["標記已讀", "查看詳情", "重發通知", "通知設定"],
    cards: [
      { label: "全部通知", title: "128 則", text: "今日新增 18 則", value: "正常" },
      { label: "未讀通知", title: "12 則", text: "高優先級 4 則", value: "待處理" },
      { label: "提醒規則", title: "5 條", text: "房款、租金、文件", value: "啟用" }
    ],
    detailBlocks: detailBlocks(
      { title: "通知分類", items: [["房款通知", "啟用"], ["租金通知", "啟用"], ["文件到期", "啟用"]] },
      { title: "發送方式", items: [["Email", "正常"], ["LINE", "正常"], ["系統通知", "正常"]] }
    )
  },
  {
    id: "ownerDocuments", shell: "owner-shell", code: "07", name: "文件資料", avatar: false,
    title: "文件資料", category: "DOCUMENTS", hero: "業主端文件資料中心",
    hint: "集中管理您的房產相關文件，安全存儲，隨時查閱。",
    table: "文件資料列表", searchHint: "搜尋文件名稱、類型、單位",
    primaryAction: "上傳文件", secondaryAction: "下載文件",
    flowTitle: "文件管理流程", flowHint: "文件上傳、審核、到期提醒和下載全程管理。",
    quickActions: ["上傳文件", "下載文件", "查看到期", "補件提醒"],
    cards: [
      { label: "全部文件", title: "128 份", text: "合約、憑證、租約", value: "已完成" },
      { label: "待補件", title: "6 份", text: "付款憑證與身份文件", value: "待處理" },
      { label: "即將到期", title: "12 份", text: "租約與保險文件", value: "提醒中" }
    ],
    detailBlocks: detailBlocks(
      { title: "文件分類", items: [["買賣合約", "已完成"], ["身份文件", "已完成"], ["付款憑證", "待處理"]] },
      { title: "文件提醒", items: [["到期提醒", "啟用"], ["補件提醒", "啟用"], ["審核狀態", "待確認"]] }
    )
  },
  {
    id: "ownerMore", shell: "owner-shell", code: "08", name: "更多服务", mobileOnly: true, avatar: false,
    title: "更多服务", category: "MORE SERVICES", hero: "业主查询服务",
    hint: "查看房款、合同、房产资料与账户服务。", table: "服务入口", searchHint: "搜索服务"
  },
  {
    id: "adminSmartDashboard", shell: "admin-shell", code: "-01", name: "智慧大屏", avatar: false,
    title: "智慧大屏", category: "SMART PORTFOLIO BOARD", hero: "集中查看马来西亚各州属的房产与出租经营数据。",
    hint: "地图和指标均来自当前数据库实时汇总。", table: "地区经营地图", searchHint: "搜索州属",
    primaryAction: "重新载入", secondaryAction: ""
  },
  {
    id: "adminDashboard", shell: "admin-shell", code: "00", name: "管理总览", avatar: false,
    title: "管理总览", category: "ADMIN DASHBOARD", hero: "按区域查看在管房产、入住率、租金、租客押金与备用金。",
    hint: "数据来自当前数据库实时汇总。", table: "区域经营概览", searchHint: "搜索区域",
    primaryAction: "重新载入", secondaryAction: ""
  },
  {
    id: "adminProjects", shell: "admin-shell", code: "00.5", name: "建案管理", avatar: false,
    title: "建案管理", category: "ADMIN PROJECT", hero: "集中建立與維護建案主資料。",
    hint: "建案資料會同步供房產、房款及租賃模組使用。", table: "建案列表", searchHint: "搜尋建案編碼、名稱、城市或地址",
    primaryAction: "", secondaryAction: ""
  },
  {
    id: "adminOwners", shell: "admin-shell", code: "01", name: "業主管理", avatar: true,
    title: "業主管理", category: "ADMIN OWNER", hero: "登記與維護所有業主資料。",
    hint: "查看全部業主、聯絡資料、帳號狀態與名下房產數量。",
    table: "業主列表", searchHint: "搜尋業主姓名 / 手機號 / 郵箱",
    primaryAction: "新增業主", secondaryAction: "",
    flowTitle: "業主建檔流程", flowHint: "建立業主、綁定單位、上傳文件、同步業主端。",
    quickActions: ["編輯資料", "查看房款", "查看預備金", "上傳文件"],
    detailBlocks: detailBlocks(
      { title: "業主資訊", items: [["證件號", "900101-14-1234"], ["手機", "+60 12-345 6789"], ["郵箱", "weiming.tan@example.com"]] },
      { title: "名下房產", items: [["A-28-05", "正常"], ["A-45-01", "正常"], ["付款憑證", "待處理"]] }
    )
  },
  {
    id: "adminProperties", shell: "admin-shell", code: "02", name: "房產管理", avatar: false,
    title: "房產管理", category: "ADMIN PROPERTY", hero: "集中管理全部房屋與單位。",
    hint: "查看所有房產、所屬業主、建案單位、交房與付款狀態。",
    table: "房產列表", searchHint: "搜尋建案 / 單位 / 業主",
    primaryAction: "新增房產", secondaryAction: "",
    flowTitle: "房產管理流程", flowHint: "建立房產、綁定業主、維護狀態與付款資料。",
    quickActions: ["新增房產", "修改房產", "查看業主", "查看房款"],
    detailBlocks: detailBlocks(
      { title: "房產資料", items: [["建案", "Pavilion Square"], ["單位", "A-28-05"], ["房產階段", "已交房"]] },
      { title: "業主與付款", items: [["業主", "—"], ["已繳", "—"], ["未繳", "—"]] }
    )
  },
  {
    id: "adminOffMarketProperties", shell: "admin-shell", code: "03.1", name: "下架房源", avatar: false,
    title: "下架房源", category: "RENTAL LISTINGS", hero: "管理已下架的出租房源资料。",
    hint: "历史租约、账单、附件与维修资料完整保留，需要时可重新上架。",
    table: "下架房源列表", searchHint: "搜索建案、单位或业主", primaryAction: "", secondaryAction: ""
  },
  {
    id: "adminProcess", shell: "admin-shell", code: "02.5", name: "租赁智控台", avatar: false,
    title: "租赁智控台", category: "ADMIN PROPERTY WORKFLOW", hero: "先選房產，再按流程完成出租作業。",
    hint: "集中查看出租委託、租客、租約、收款及後續營運進度。",
    table: "房產流程", searchHint: "搜尋建案 / 單位 / 業主", primaryAction: "選擇房產", secondaryAction: "重新載入",
    flowTitle: "房產出租流程", flowHint: "每個步驟都能直接進入對應功能。", quickActions: [], detailBlocks: []
  },
  {
    id: "adminRentalSigning", shell: "admin-shell", code: "02.6", name: "附件签约", avatar: false,
    title: "附件签约", category: "ADMIN RENTAL SIGNING", hero: "集中生成出租附件并处理电子签署。",
    hint: "授权书、OTR 与租赁合同在此生成、签署和归档。", table: "附件签约", searchHint: "搜索文件、委托编号或房产",
    primaryAction: "", secondaryAction: ""
  },
  {
    id: "adminDeposits", shell: "admin-shell", code: "02.7", name: "押金管理", avatar: false,
    title: "押金管理", category: "ADMIN RENTAL DEPOSITS", hero: "独立管理租约押金余额与全部异动。",
    hint: "集中处理押金代付、归还、退款、没收和余额调整。", table: "押金账户", searchHint: "搜索租客、租约或房产",
    primaryAction: "", secondaryAction: ""
  },
  {
    id: "adminTenantDirectory", shell: "admin-shell", code: "02.8", name: "租客管理", avatar: true,
    title: "租客管理", category: "ADMIN TENANT DIRECTORY", hero: "集中维护租客资料、联络方式与租约状态。",
    hint: "有租约记录的租客可停用，但不能删除。", table: "租客列表", searchHint: "搜索姓名／证件号／电话／邮箱",
    primaryAction: "新增租客", secondaryAction: ""
  },
  {
    id: "adminTenants", shell: "admin-shell", code: "03", name: "租客與租金", avatar: true,
    title: "租客與租金管理", category: "ADMIN TENANCY", hero: "管理租客資料、租約、每月租金、已收未收和合約到期。",
    hint: "對應設計稿中的租客與租金管理頁。",
    table: "租客與租金列表", searchHint: "搜尋租客 / 單位 / 租約",
    primaryAction: "新增租客", secondaryAction: "新增租約",
    flowTitle: "租金管理流程", flowHint: "租約建立、每月出帳、線下收款登記、憑證存檔與到期提醒。",
    quickActions: ["查看租約", "上傳憑證", "發送通知", "上傳文件"],
    detailBlocks: detailBlocks(
      { title: "租客資料", items: [["租客", "Rachel Lee"], ["單位", "A-19-09"], ["合約到期", "2025-09-30"]] },
      { title: "租金狀態", items: [["本月租金", "待收"], ["押金", "已完成"], ["通知", "啟用"]] }
    )
  },
  {
    id: "adminMaintenance", shell: "admin-shell", code: "05", name: "收支與維修", avatar: false,
    title: "收支與維修管理", category: "ADMIN MAINTENANCE", hero: "維修、水電、管理費、其他支出、憑證附件與支付狀態。",
    hint: "對應設計稿中的收支與維修管理頁。",
    table: "收支與維修列表", searchHint: "搜尋工單 / 支出 / 供應商",
    primaryAction: "新增支出", secondaryAction: "新增維修",
    flowTitle: "維修支出流程", flowHint: "建立工單、分類、上傳憑證、審核、付款。",
    quickActions: ["建立工單", "上傳憑證", "財務確認", "同步會計"],
    detailBlocks: detailBlocks(
      { title: "工單資訊", items: [["分類", "維修費用"], ["供應商", "CoolMax Service"], ["附件", "已完成"]] },
      { title: "處理狀態", items: [["審核", "待確認"], ["支付", "待處理"], ["同步", "待處理"]] }
    )
  },
  {
    id: "adminRentalMandates", shell: "admin-shell", code: "03A", name: "出租委託", avatar: false,
    title: "出租委託管理", category: "ADMIN RENTAL MANDATE", hero: "承接業主出租意向，建立正式委託、上傳文件、審核並啟用租管服務。",
    hint: "業主端只設定出租意向；管理端負責正式委託、接管與審核。", table: "出租委託列表", searchHint: "搜尋委託編號 / 業主 / 單位",
    primaryAction: "建立委託", secondaryAction: "重新載入", flowTitle: "委託流程", flowHint: "建立草稿、提交審核、核准啟用、暫停或終止。"
  },
  {
    id: "adminFinance", shell: "admin-shell", code: "04", name: "財務確認", avatar: false,
    title: "財務確認管理", category: "ADMIN FINANCE", hero: "管理待確認、部分收款、已確認、未付款、部分付款與同步狀態。",
    hint: "對應設計稿中的財務確認管理頁。",
    table: "財務確認列表", searchHint: "搜尋交易編號 / 付款人 / 單位",
    primaryAction: "重新載入", secondaryAction: "批量確認",
    flowTitle: "財務確認流程", flowHint: "憑證上傳、銀行核對、財務確認、同步 SQL Account。",
    quickActions: ["確認收款", "退回補件", "部分付款", "同步會計"],
    detailBlocks: detailBlocks(
      { title: "付款資訊", items: [["交易編號", "RC-250501"], ["付款人", "Tan Wei Ming"], ["銀行", "Maybank"]] },
      { title: "確認狀態", items: [["付款憑證", "已完成"], ["財務確認", "待確認"], ["SQL 同步", "待處理"]] }
    )
  },
  {
    id: "adminData", shell: "admin-shell", code: "02", name: "建築與房款", avatar: false,
    title: "建築與房款管理", category: "ADMIN BUILDING", hero: "管理建案、房型、單位、房款總價、分期與付款進度。",
    hint: "對應設計稿中的建築與房款管理頁。",
    table: "建築與房款列表", searchHint: "搜尋建案 / 單位 / 房款狀態",
    primaryAction: "新增建案", secondaryAction: "新增房款",
    flowTitle: "建築房款流程", flowHint: "建立建案單位、設定房款與分期、追蹤財務確認。",
    quickActions: ["新增建案", "新增單位", "設定房款", "同步業主端"],
    detailBlocks: detailBlocks(
      { title: "資料類型", items: [["建案", "正常"], ["單位", "正常"], ["供應商", "待確認"]] },
      { title: "引用狀態", items: [["房產", "正常"], ["維修", "正常"], ["報表", "正常"]] }
    )
  },
  {
    id: "adminReserve", shell: "admin-shell", code: "06", name: "預備金", avatar: false,
    title: "預備金管理", category: "ADMIN RESERVE", hero: "管理屋主預備金餘額、充值、扣款與不足提醒。",
    hint: "對應設計稿左側預備金模組。",
    table: "預備金列表", searchHint: "搜尋屋主 / 單位 / 餘額狀態",
    primaryAction: "新增充值", secondaryAction: "新增扣款",
    flowTitle: "預備金流程", flowHint: "設定最低標準、充值扣款、自動計算餘額。",
    quickActions: ["充值記錄", "扣款記錄", "設定標準", "發送通知"],
    detailBlocks: detailBlocks(
      { title: "預備金設定", items: [["最低標準", "RM 20,000"], ["當前餘額", "RM 25,000"], ["不足提醒", "啟用"]] },
      { title: "異動紀錄", items: [["充值", "已完成"], ["扣款", "已完成"], ["餘額", "正常"]] }
    )
  },
  {
    id: "adminAlerts", shell: "admin-shell", code: "07", name: "自動提醒", avatar: false,
    title: "自動提醒", category: "ADMIN REMINDER", hero: "管理房款、租金、預備金、維修與文件到期提醒。",
    hint: "對應設計稿中的自動提醒 / 通知中心。",
    table: "提醒規則與發送紀錄", searchHint: "搜尋提醒場景 / 對象 / 發送狀態",
    primaryAction: "新增提醒", secondaryAction: "批量發送",
    flowTitle: "提醒流程", flowHint: "設定規則、發送通知、記錄成功與失敗原因。",
    quickActions: ["新增規則", "立即發送", "查看失敗", "重發通知"],
    detailBlocks: detailBlocks(
      { title: "提醒場景", items: [["房款到期", "啟用"], ["租金到期", "啟用"], ["文件到期", "啟用"]] },
      { title: "通知方式", items: [["Email", "正常"], ["LINE", "正常"], ["系統通知", "正常"]] }
    )
  },
  {
    id: "adminReports", shell: "admin-shell", code: "09", name: "報表與導出", avatar: false,
    title: "報表與導出", category: "ADMIN REPORTS", hero: "產生租金收款進度、租金收支、維修費用、預備金與提醒紀錄。",
    hint: "對應設計稿中的報表與導出頁。",
    table: "報表任務列表", searchHint: "搜尋報表名稱 / 週期",
    primaryAction: "新增報表", secondaryAction: "批量導出",
    flowTitle: "報表產出流程", flowHint: "選擇範圍、產生 PDF/XLSX、下載並歸檔。",
    quickActions: ["產生 PDF", "產生 XLSX", "排程報表", "歸檔"],
    detailBlocks: detailBlocks(
      { title: "報表內容", items: [["租金收款", "已完成"], ["維修費用", "待確認"], ["預備金", "正常"]] },
      { title: "導出狀態", items: [["PDF", "正常"], ["XLSX", "正常"], ["排程", "啟用"]] }
    )
  },
  {
    id: "adminAccounts", shell: "admin-shell", code: "09.5", name: "管理员账号", avatar: false,
    title: "管理员账号", category: "ADMIN ACCESS CONTROL", hero: "管理五类后台岗位账号与登录状态。",
    hint: "岗位权限会同时作用于菜单、操作按钮与后端接口。", table: "后台账号列表", searchHint: "搜索账号、姓名、岗位或联系方式",
    primaryAction: "新增管理员", secondaryAction: ""
  },
  {
    id: "adminAudit", shell: "admin-shell", code: "10", name: "操作審計", avatar: false,
    title: "操作審計中心", category: "ADMIN AUDIT", hero: "集中查看後台資料的新增、修改與停用紀錄。",
    hint: "可按時間、文員、操作類型與關鍵字追查資料異動。",
    table: "操作審計紀錄", searchHint: "搜尋操作、文員或編號",
    primaryAction: "", secondaryAction: "",
    flowTitle: "操作追蹤流程", flowHint: "每次受記錄的資料異動，都保留操作者、時間與修改前後內容。"
  },
  {
    id: "adminSystemBackup", shell: "admin-shell", code: "11", name: "備份與恢復", avatar: false,
    title: "備份與恢復", category: "SYSTEM BACKUP", hero: "備份資料庫與全部上傳文件，並在需要時安全恢復整個系統。",
    hint: "恢復前會自動產生安全備份，避免誤操作後無法回退。",
    table: "備份記錄", searchHint: "搜尋備份文件",
    primaryAction: "", secondaryAction: ""
  },
];

const featureMap = {
  myProperties: [["A1", "房產總覽", ["已購房產與單位資料", "建案、房型、租住狀態", "房款、租金、預備金入口"]], ["A2", "業主資料透明", ["房款進度可查", "收入支出可查", "文件狀態可查"]], ["A3", "通知入口", ["房款到期", "租金通知", "文件與預備金提醒"]]],
  ownerPayment: [["B1", "房款進度", ["房產總價", "總繳款期數 / 已繳期數", "剩餘期數與未繳金額"]], ["B2", "每期明細", ["每期應繳金額", "到期日", "財務確認狀態"]], ["B3", "憑證留存", ["付款憑證", "上傳日期", "審核結果"]]],
  rentIncome: [["C1", "租金收入", ["每月租金收入", "已收 / 未收租金", "租客付款狀態"]], ["C2", "租約資料", ["租客資料", "租約期限", "合約到期提醒"]], ["C3", "歷史紀錄", ["租金歷史", "收款憑證", "財務確認"]]],
  ownerExpenses: [["D1", "支出明細", ["維修費用", "水電費", "管理費與其他支出"]], ["D2", "憑證說明", ["支出憑證", "維修附件", "費用說明"]], ["D3", "淨收入", ["收入支出對照", "扣款記錄", "月度結算"]]],
  ownerReserve: [["E1", "預備金餘額", ["當前餘額", "最低標準", "不足提醒"]], ["E2", "充值扣款", ["充值記錄", "扣款記錄", "自動計算餘額"]], ["E3", "通知中心", ["不足通知", "維修扣款通知", "補繳提醒"]]],
  ownerNotice: [["F1", "通知分類", ["房款收款通知", "租金收款通知", "合約文件到期提醒"]], ["F2", "發送管道", ["Email", "LINE", "系統內通知"]], ["F3", "通知狀態", ["已讀 / 未讀", "發送成功", "失敗原因"]]],
  ownerDocuments: [["G1", "文件分類", ["買賣合約", "租約", "付款憑證"]], ["G2", "狀態追蹤", ["已上傳", "待補件", "即將到期"]], ["G3", "操作", ["上傳", "下載", "到期提醒"]]],
  adminAccounts: [["A1", "帳號資料", ["管理員與業主登入帳號", "帳號狀態", "關聯業主"]], ["A2", "安全管理", ["密碼加密", "重設密碼", "獨立登入入口"]], ["A3", "操作", ["新增", "修改", "停用或刪除"]]],
  adminProjects: [],
  adminRentalSigning: [],
  adminOwners: [["H1", "業主資料", ["登記業主基本資料", "管理名下單位", "上傳合約文件"]], ["H2", "房產概覽", ["房產總價", "已繳 / 未繳", "預備金狀態"]], ["H3", "文件狀態", ["買賣合約", "身份文件", "付款憑證"]]],
  adminProperties: [["P1", "房產主檔", ["建案與單位", "房型與面積", "交房狀態"]], ["P2", "業主關聯", ["所屬業主", "持有比例", "持有日期"]], ["P3", "財務狀態", ["房產總價", "已繳金額", "未繳金額"]]],
  adminOffMarketProperties: [],
  adminTenantDirectory: [],
  adminTenants: [["I1", "租客資料", ["租客基本資料", "租約資料", "押金資料"]], ["I2", "租金管理", ["每月租金", "已收未收", "逾期提醒"]], ["I3", "合約提醒", ["租約到期", "文件到期", "續約跟進"]]],
  adminMaintenance: [["J1", "支出分類", ["維修費用", "水電費", "管理費", "其他支出"]], ["J2", "工單追蹤", ["處理進度", "供應商", "維修附件"]], ["J3", "財務確認", ["付款憑證", "支付狀態", "同步會計"]]],
  adminFinance: [["K1", "確認狀態", ["待確認", "部分收款", "已確認收款"]], ["K2", "付款狀態", ["未付款", "部分付款", "已付款"]], ["K3", "會計同步", ["已同步", "同步失敗", "失敗原因"]]],
  adminData: [["L1", "主檔資料", ["建案", "房型", "單位"]], ["L2", "營運字典", ["費用類型", "狀態類型", "供應商"]], ["L3", "資料引用", ["支援房產", "支援維修", "支援報表"]]],
  adminReserve: [["R1", "預備金帳戶", ["當前餘額", "最低標準", "充值扣款"]], ["R2", "不足提醒", ["低於最低標準", "自動通知", "補繳追蹤"]], ["R3", "扣款關聯", ["維修扣款", "支出扣款", "餘額重新計算"]]],
  adminAlerts: [["A1", "自動提醒場景", ["房款到期", "租金到期", "文件到期"]], ["A2", "通知方式", ["Email", "LINE", "系統內通知"]], ["A3", "通知後台", ["發送對象", "發送內容", "失敗原因"]]],
  adminReports: [["M1", "租金報表", ["租金收款進度", "租金收支記錄", "未收款提醒"]], ["M2", "支出報表", ["維修費用", "支出明細", "付款狀態"]], ["M3", "導出格式", ["PDF", "XLSX", "排程產生"]]],
  adminAudit: [["O1", "查詢條件", ["日期區間", "操作人員", "操作類型"]], ["O2", "追蹤內容", ["受影響資料", "修改前資料", "修改後資料"]], ["O3", "資料保護", ["不可修改", "不可刪除", "管理員查閱"]]]
};

const workflowMap = {
  adminAccounts: ["新增帳號", "設定類型", "修改資料", "停用帳號", "操作留痕"],
  myProperties: ["登入平台", "查看房產", "進入明細", "接收提醒", "下載文件"],
  ownerPayment: ["查看總價", "查看分期", "上傳憑證", "等待確認", "完成紀錄"],
  rentIncome: ["租客付款", "財務確認", "業主查詢", "下載收據", "歷史歸檔"],
  ownerExpenses: ["新增支出", "上傳憑證", "審核確認", "業主查看", "月度結算"],
  ownerReserve: ["設定標準", "充值", "扣款", "計算餘額", "不足提醒"],
  ownerNotice: ["觸發規則", "發送通知", "業主讀取", "失敗重發", "留存紀錄"],
  ownerDocuments: ["上傳文件", "審核", "分類", "到期提醒", "業主下載"],
  adminProjects: [],
  adminRentalSigning: [],
  adminOwners: ["新增業主", "綁定房產", "上傳文件", "檢查狀態", "同步業主端"],
  adminProperties: ["新增房產", "綁定業主", "維護資料", "檢查狀態", "查看房款"],
  adminOffMarketProperties: [],
  adminTenantDirectory: ["維護資料", "建立租客", "管理狀態"],
  adminTenants: ["新增租客", "建立租約", "每月租金", "收款確認", "到期提醒"],
  adminMaintenance: ["建立工單", "分類支出", "上傳憑證", "審核付款", "同步會計"],
  adminFinance: ["收到憑證", "核對銀行", "確認狀態", "同步 SQL", "留存紀錄"],
  adminData: ["建立主檔", "維護字典", "引用資料", "檢查完整", "開放使用"],
  adminReserve: ["設定標準", "充值", "扣款", "計算餘額", "不足提醒"],
  adminAlerts: ["設定規則", "匹配對象", "發送通知", "記錄結果", "失敗重發"],
  adminReports: ["選擇報表", "設定範圍", "產生檔案", "下載", "歸檔"],
  adminAudit: ["產生異動", "記錄操作人員", "保存前後資料", "條件查詢", "查看詳情"]
};

const reminderMap = {
  adminAccounts: ["停用帳號提醒", "密碼重設", "重複帳號檢查"],
  myProperties: ["房款到期提醒", "租金收款通知", "預備金不足通知"],
  ownerPayment: ["房款即將到期", "房款已逾期", "財務未確認"],
  rentIncome: ["租金即將到期", "租金已逾期", "租約即將到期"],
  ownerExpenses: ["維修費用通知", "支出憑證通知", "月度結算通知"],
  ownerReserve: ["預備金不足", "扣款通知", "補繳提醒"],
  ownerNotice: ["Email 發送失敗", "LINE 通知失敗", "重複發送待確認"],
  ownerDocuments: ["合約文件到期", "文件待補件", "文件審核完成"],
  adminProjects: [],
  adminRentalSigning: [],
  adminOwners: ["屋主資料缺失", "合約文件即將到期", "付款憑證待補件"],
  adminProperties: ["房產資料缺失", "未綁定業主", "交房狀態待更新"],
  adminOffMarketProperties: [],
  adminTenants: ["租金收款通知", "租約到期提醒", "租客文件缺失"],
  adminMaintenance: ["維修費用需確認", "支出憑證缺失", "付款待審核"],
  adminFinance: ["付款後未確認", "部分收款需追蹤", "同步會計失敗"],
  adminData: ["基礎資料缺欄", "供應商待審核", "狀態字典未同步"],
  adminReserve: ["預備金不足", "扣款後通知", "補繳未完成"],
  adminAlerts: ["Email 發送失敗", "LINE 發送失敗", "重複發送提醒"],
  adminReports: ["月報產生", "導出完成", "未收款提醒紀錄"],
  adminAudit: ["重要異動已保留", "可依文員追查", "舊資料未留存無法補回"]
};

const adminKpis = {
  adminOwners: [
    ["人", "業主總數", "128 位業主", "↑ 6.7% 較上月", "up"],
    ["房", "名下房產總數", "256 套", "↑ 5.4% 較上月", "up"],
    ["樓", "在管房產", "238 套", "↑ 4.2% 較上月", "up"],
    ["款", "有未繳款業主", "18 位", "↓ 15.8% 較上月", "down"],
    ["!", "預備金不足業主", "12 位", "↓ 7.7% 較上月", "down"]
  ],
  adminProperties: [
    ["房", "房產總數", "0 套", "資料庫即時統計", ""],
    ["樓", "已交房", "0 套", "資料庫即時統計", ""],
    ["期", "未交房", "0 套", "資料庫即時統計", ""],
    ["款", "尚有未繳", "0 套", "資料庫即時統計", ""]
  ],
  adminData: [
    ["屋", "房產總價", "RM 2,500,000.00", "", "up"],
    ["✓", "已繳總金額", "RM 1,300,000.00", "", "up"],
    ["!", "未繳總金額", "RM 1,200,000.00", "", "down"],
    ["期", "總繳費期數", "10 期", "", "up"],
    ["期", "已繳期數", "4 期", "", "up"],
    ["期", "剩餘期數", "6 期", "", "down"],
    ["$", "下一期金額", "RM 150,000.00", "", "down"],
    ["日", "下一期到期日", "2026-07-15", "", "down"]
  ],
  adminTenants: [
    ["人", "租客總數", "128 位租客", "↑ 6.7%", "up"],
    ["租", "本月應收租金", "RM 428,500.00", "↑ 3.8%", "up"],
    ["✓", "本月已收租金", "RM 265,200.00", "↑ 12.4%", "up"],
    ["!", "未收金額", "RM 163,300.00", "↓ 6.1%", "down"],
    ["部", "部分收款筆數", "18 筆", "↑ 2", "up"],
    ["逾", "逾期租金筆數", "12 筆", "↓ 3", "down"],
    ["財", "待財務確認", "21 筆", "↑ 4", "down"]
  ],
  adminFinance: [
    ["收", "待確認收款", "18 筆", "需核對", "down"],
    ["✓", "已確認收款", "42 筆", "本月完成", "up"],
    ["部", "部分收款", "7 筆", "需追蹤", "down"],
    ["!", "未付款", "9 筆", "需提醒", "down"],
    ["款", "已付款", "36 筆", "可同步", "up"],
    ["財", "待同步 SQL", "6 筆", "需處理", "down"],
    ["$", "本月確認金額", "RM 265,200.00", "↑ 12.4%", "up"]
  ],
  adminMaintenance: [
    ["收", "本月總收入", "RM 1,280,000.00", "↑ 12.35%", "up"],
    ["支", "本月總支出", "RM 254,630.00", "↑ 8.71%", "down"],
    ["修", "維修支出", "RM 58,320.00", "↑ 15.20%", "down"],
    ["水", "水電費", "RM 38,450.00", "↓ 6.45%", "up"],
    ["管", "管理費", "RM 27,860.00", "↑ 3.18%", "down"],
    ["待", "待確認記錄", "12 條記錄", "金額 RM 96,750.00", "down"],
    ["餘", "從預備金扣款金額", "RM 42,800.00", "本月累計", "up"]
  ],
  adminReserve: [
    ["錢", "預備金總餘額", "RM 4,385,600.00", "", "up"],
    ["人", "低於標準業主數", "23 戶", "占比 8.62%", "down"],
    ["入", "本月累計充值", "RM 285,600.00", "↑ 18.54%", "up"],
    ["出", "本月累計扣款", "RM 112,350.00", "↓ 12.23%", "down"],
    ["待", "待財務確認", "RM 68,900.00", "共 15 筆記錄", "down"],
    ["盾", "最低標準設置數", "56 個單位", "未設置 4 個單位", "up"]
  ],
  adminReports: [
    ["收", "本月總收款", "RM 2,580,000.00", "↑ 18.6%", "up"],
    ["支", "本月總支出", "RM 1,125,300.00", "↑ 9.2%", "up"],
    ["欠", "未收房款", "RM 1,200,000.00", "↓ 6.5%", "down"],
    ["租", "未收租金", "RM 210,000.00", "↓ 3.1%", "up"],
    ["修", "維修費用", "RM 185,600.00", "↑ 12.8%", "up"],
    ["餘", "預備金餘額", "RM 2,500,000.00", "↑ 5.4%", "up"],
    ["同", "SQL 同步成功率", "98.6%", "↑ 1.2%", "up"]
  ]
};

const rows = {
  adminProjects: [],
  adminAccounts: [],
  adminProcess: [],
  adminRentalSigning: [],
  myProperties: [["Pavilion Square", "A-28-05", "2 Bedroom", "1,088 sq.ft", "1,300,000.00", "1,200,000.00", "100,000.00", "正常"], ["CCP Residence", "B-12-07", "Studio", "680 sq.ft", "850,000.00", "600,000.00", "250,000.00", "待確認"], ["CCPS Heights", "A-19-09", "Investment", "1,240 sq.ft", "3,180,000.00", "3,030,000.00", "150,000.00", "出租中"]],
  ownerPayment: [["A-28-05", "第 10 期", "100,000.00", "2025-04-15", "已完成", "已完成", "RC-250410"], ["A-28-05", "第 11 期", "100,000.00", "2025-05-15", "已完成", "已完成", "RC-250515"], ["A-28-05", "第 12 期", "100,000.00", "2025-06-15", "待確認", "待確認", "RC-250615"]],
  rentIncome: [["Mohd Hakim", "A-28-05", "2025-05", "4,800.00", "已完成", "2025-05-03", "正常"], ["Rachel Lee", "A-19-09", "2025-05", "5,200.00", "待確認", "2025-05-06", "待處理"], ["Chen Yi Fan", "B-12-07", "2025-05", "3,200.00", "已完成", "2025-05-02", "正常"]],
  ownerExpenses: [["WO-10091", "維修費用", "A-28-05", "CoolMax Service", "580.00", "已完成", "已完成", "已完成"], ["EX-10092", "管理費", "Pavilion Square", "CleanPro", "3,600.00", "已完成", "待確認", "待處理"], ["WO-10093", "水電費", "C-20-08", "PipeCare", "920.00", "待處理", "待確認", "處理中"]],
  ownerReserve: [["A-28-05", "25,000.00", "25,000.00", "2025-05-22", "580.00", "正常", "啟用"], ["B-12-07", "20,000.00", "6,500.00", "2025-04-03", "13,500.00", "不足", "啟用"], ["C-10-02", "25,000.00", "8,000.00", "2025-03-19", "17,000.00", "不足", "啟用"]],
  ownerNotice: [["房款即將到期", "A-28-05", "2025-06-15", "Email + LINE", "啟用", "-", "今天"], ["預備金不足", "B-12-07", "低於 RM 20,000", "系統內通知", "啟用", "-", "今天"], ["文件待補件", "A-28-05", "付款憑證", "Email", "待處理", "收件人缺失", "昨天"]],
  ownerDocuments: [["買賣合約", "A-28-05", "PDF", "2025-01-02", "已完成", "2026-01-02", "正常"], ["付款憑證", "A-28-05", "JPG", "2025-05-15", "待處理", "-", "待補件"], ["租約文件", "A-19-09", "PDF", "2024-09-30", "已完成", "2025-09-30", "即將到期"]],
  adminOwners: [["Tan Wei Ming", "+60 12-345 6789", "Pavilion Square", "A-28-05", "正常", "1,300,000.00", "1,200,000.00", "100,000.00", "已完成"], ["Lim Jia Wei", "+60 13-987 6543", "Pavilion Square", "B-12-07", "待處理", "850,000.00", "600,000.00", "250,000.00", "待處理"], ["Wong Kok Leong", "+60 16-223 5566", "Pavilion Square", "A-19-09", "正常", "3,180,000.00", "3,030,000.00", "150,000.00", "已完成"], ["Nur Farah Binti Ali", "+60 11-445 6677", "Pavilion Square", "C-10-02", "待處理", "2,650,000.00", "2,400,000.00", "250,000.00", "待確認"], ["Ho Chin Seng", "+60 12-778 8890", "Pavilion Square", "B-30-01", "正常", "3,500,000.00", "3,500,000.00", "0.00", "已完成"], ["G R Alphyshah Bt Rahman", "+60 17-889 9012", "Pavilion Square", "D-10-02", "待處理", "2,400,000.00", "1,200,000.00", "1,200,000.00", "待處理"], ["Yap Soon Huat", "+60 13-223 4455", "Pavilion Square", "A-30-08", "正常", "3,000,000.00", "3,000,000.00", "0.00", "已完成"], ["Lee Mei Ling", "+60 16-556 7788", "Pavilion Square", "C-20-08", "待處理", "2,800,000.00", "2,100,000.00", "700,000.00", "待處理"], ["Ong Mei Ling", "+60 12-334 5566", "Pavilion Square", "A-45-01", "正常", "1,180,000.00", "1,180,000.00", "0.00", "已完成"], ["Steven Tan", "+60 14-667 8899", "Pavilion Square", "B-20-01", "待處理", "860,000.00", "610,000.00", "250,000.00", "待處理"]],
  adminProperties: [],
  adminOffMarketProperties: [],
  adminTenantDirectory: [],
  adminTenants: [["Tan Wei Ming", "Pavilion Square / A-28-05", "2024-12-15 ~ 2025-12-14", "2,800.00", "2026-05-15", "2,800.00", "0.00", "0", "已收", "有", "已確認"], ["Lim Jia Hui", "Pavilion Square / B-12-03", "2025-02-01 ~ 2026-01-31", "3,200.00", "2026-05-05", "1,600.00", "1,600.00", "0", "部分收款", "有", "待財務確認"], ["Wong Kok Leong", "Pavilion Square / A-18-09", "2024-11-01 ~ 2025-10-31", "3,000.00", "2026-05-01", "0.00", "3,000.00", "14", "逾期", "-", "待財務確認"], ["Nur Farah Binti Ali", "Pavilion Square / C-05-12", "2025-03-01 ~ 2026-02-28", "2,600.00", "2026-05-01", "0.00", "2,600.00", "14", "逾期", "-", "待確認"], ["Ho Chin Seng", "Pavilion Square / B-23-01", "2024-09-15 ~ 2025-09-14", "3,500.00", "2026-05-01", "3,500.00", "0.00", "0", "已收", "有", "已確認"], ["Siti Aisyah Bt Rahman", "Pavilion Square / A-07-02", "2025-02-10 ~ 2026-02-09", "2,400.00", "2026-05-01", "1,200.00", "1,200.00", "5", "部分收款", "有", "待財務確認"], ["Yap Soon Huat", "Pavilion Square / B-16-01", "2024-01-01 ~ 2025-09-30", "3,000.00", "2026-05-01", "0.00", "3,000.00", "14", "逾期", "-", "-"], ["Lee Mei Ling", "Pavilion Square / C-20-08", "2025-04-15 ~ 2026-04-14", "2,900.00", "2026-05-01", "0.00", "2,900.00", "0", "待收", "-", "-"], ["Muhammad Izwan", "Pavilion Square / A-33-11", "2025-02-20 ~ 2026-02-19", "2,700.00", "2026-05-01", "2,700.00", "0.00", "0", "已收", "有", "待財務確認"], ["Chan Pui San", "Pavilion Square / B-30-06", "2024-08-01 ~ 2025-07-31", "3,300.00", "2026-05-01", "0.00", "3,300.00", "14", "逾期", "-", "-"]],
  adminMaintenance: [["WO-10091", "維修費用", "A-28-05", "CoolMax Service", "580.00", "已完成", "已完成", "已完成"], ["EX-10092", "管理費", "Pavilion Square", "CleanPro", "3,600.00", "已完成", "待確認", "待處理"], ["WO-10093", "水電費", "C-20-08", "PipeCare", "920.00", "待處理", "待確認", "處理中"]],
  adminFinance: [["RC-250501", "房款", "A-28-05", "Tan Wei Ming", "100,000.00", "已完成", "待確認", "待處理"], ["RC-250502", "租金", "A-19-09", "Rachel Lee", "5,200.00", "已完成", "已確認", "已同步"], ["RF-250503", "退款", "B-12-07", "Lim Jia Wei", "2,000.00", "待處理", "待確認", "待處理"], ["RC-250504", "房款", "B-30-01", "Ho Chin Seng", "250,000.00", "已完成", "已確認", "待同步"], ["RC-250505", "維修", "C-20-08", "Lee Mei Ling", "2,900.00", "待補件", "未付款", "未同步"], ["RC-250506", "租金", "A-07-02", "Siti Aisyah", "1,200.00", "已完成", "部分收款", "待處理"], ["RC-250507", "房款", "D-10-02", "G R Alphyshah", "150,000.00", "已完成", "部分付款", "同步失敗"], ["RC-250508", "預備金", "A-45-01", "Ong Mei Ling", "80,000.00", "已完成", "已付款", "已同步"], ["RC-250509", "租金", "B-16-01", "Yap Soon Huat", "3,000.00", "待處理", "未付款", "未同步"], ["RC-250510", "房款", "A-33-11", "Muhammad Izwan", "120,000.00", "已完成", "已確認", "已同步"]],
  adminData: [["1. 定金", "2026-7-7", "250,000.00", "250,000.00", "0.00", "2026-7-7", "已完成", "INV-001"], ["2. 基礎工程完成", "2026-7-7", "250,000.00", "250,000.00", "0.00", "2026-7-7", "已完成", "INV-002"], ["3. 結構工程完成", "2026-7-7", "300,000.00", "150,000.00", "150,000.00", "2026-7-7", "部分付款", "INV-003"], ["4. 墻體工程完成", "2026-7-7", "250,000.00", "0.00", "250,000.00", "-", "待付款", "-"], ["5. 屋頂工程完成", "2026-07-15", "150,000.00", "0.00", "150,000.00", "-", "待付款", "-"], ["6. 門窗安裝完成", "2026-09-15", "150,000.00", "0.00", "150,000.00", "-", "待付款", "-"], ["7. 內部裝修完成", "2026-11-15", "200,000.00", "0.00", "200,000.00", "-", "待付款", "-"], ["8. 設備安裝完成", "2027-01-15", "150,000.00", "0.00", "150,000.00", "-", "待付款", "-"], ["9. 驗收與交屋", "2027-03-15", "100,000.00", "0.00", "100,000.00", "-", "待付款", "-"], ["10. 交屋尾款", "2027-04-15", "100,000.00", "0.00", "100,000.00", "-", "逾期", "-"]],
  adminReserve: [["Tan Wei Ming / A-28-05", "25,000.00", "25,000.00", "2025-05-22", "580.00", "正常", "啟用"], ["Lim Jia Wei / B-12-07", "20,000.00", "6,500.00", "2025-04-03", "13,500.00", "不足", "啟用"], ["Nur Farah / C-10-02", "25,000.00", "8,000.00", "2025-03-19", "17,000.00", "不足", "啟用"]],
  adminAlerts: [["房款即將到期", "Tan Wei Ming", "到期前 7 天", "Email + LINE", "啟用", "-", "今天"], ["租金即將到期", "Rachel Lee", "每月到期前 3 天", "Email", "啟用", "-", "昨天"], ["合約文件到期", "Nur Farah", "到期前 30 天", "Email", "待處理", "收件人缺失", "2025-05-20"]],
  adminReports: [["租金收款進度", "2025-05", "月報", "PDF", "已完成", "Admin CCPS", "可下載"], ["租金收支記錄", "2025-05", "月報", "XLSX", "已完成", "Finance", "可下載"], ["維修費用明細", "2025-Q2", "季報", "PDF", "待確認", "Ops", "等待"]]
};

rows.adminRentalMandates = [];
rows.adminAudit = [];

const headers = {
  adminProjects: [],
  adminAccounts: ["登入帳號", "顯示名稱", "手機號", "帳號類型", "關聯業主", "狀態", "操作"],
  myProperties: ["建案", "單位", "房型", "面積", "房產階段", "營運服務", "房產總價", "已繳金額", "未繳金額", "付款狀態", "操作"],
  ownerPayment: ["單位", "期數", "應繳金額", "到期日", "付款憑證", "財務確認", "收據編號", "操作"],
  rentIncome: ["租客", "單位", "月份", "月租", "收款狀態", "付款日期", "租約狀態", "操作"],
  ownerExpenses: ["支出/工單", "分類", "單位", "供應商", "金額", "憑證", "支付狀態", "處理進度", "操作"],
  ownerReserve: ["單位", "最低預備金", "目前餘額", "充值記錄", "扣款金額", "餘額狀態", "提醒", "操作"],
  ownerNotice: ["提醒場景", "提醒對象", "觸發條件", "通知方式", "發送狀態", "失敗原因", "最近發送", "操作"],
  ownerDocuments: ["文件名稱", "單位", "格式", "上傳日期", "審核狀態", "到期日", "狀態", "操作"],
  adminOwners: ["業主姓名", "手機號", "建案/項目", "單位編號", "房產狀態", "房產總價", "已繳金額", "未繳金額", "文件狀態", "操作"],
  adminProperties: ["建案／項目", "單位編號", "業主", "房型", "房產階段", "房產總價", "已繳金額", "未繳金額", "操作"],
  adminOffMarketProperties: [],
  adminProcess: [],
  adminRentalSigning: [],
  adminTenantDirectory: [],
  adminTenants: ["租客姓名", "單位編號", "月租", "租期", "本月租金", "已收/未收", "合約到期", "入住狀態", "操作"],
  adminMaintenance: ["支出/工單", "分類", "單位", "供應商", "金額", "憑證", "支付狀態", "處理進度", "操作"],
  adminFinance: ["交易編號", "款項類型", "關聯單位", "付款人", "金額", "付款憑證", "確認狀態", "同步狀態", "操作"],
  adminData: ["期數/工程階段", "到期日", "應收金額", "已收金額", "未收金額", "付款日期", "狀態", "憑證", "操作"],
  adminReserve: ["屋主/單位", "最低預備金", "目前餘額", "充值記錄", "扣款金額", "餘額狀態", "提醒", "操作"],
  adminAlerts: ["提醒場景", "提醒對象", "觸發條件", "通知方式", "發送狀態", "失敗原因", "最近發送", "操作"],
  adminReports: ["報表名稱", "資料範圍", "週期", "格式", "產生狀態", "建立人", "下載", "操作"]
};

headers.adminRentalMandates = ["委託編號", "業主", "建案／單位", "委託期間", "負責人", "狀態", "操作"];
headers.adminAudit = ["操作時間", "操作人員", "操作內容", "資料類型", "資料編號", "詳情"];

Object.assign(adminKpis, {
  adminMaintenance: [
    ["收", "本月總收入", "RM 1,280,000.00", "↑ 12.35% 較上月", "up"],
    ["支", "本月總支出", "RM 254,630.00", "↑ 8.71% 較上月", "down"],
    ["修", "維修支出", "RM 58,320.00", "↑ 15.20% 較上月", "down"],
    ["水", "水電費", "RM 38,450.00", "↓ 6.45% 較上月", "up"],
    ["管", "管理費", "RM 27,860.00", "↑ 3.18% 較上月", "down"],
    ["待", "待確認記錄", "12 條記錄", "金額 RM 96,750.00", "down"],
    ["餘", "從預備金扣款金額", "RM 42,800.00", "本月累計", "up"]
  ]
});

Object.assign(rows, {
  adminMaintenance: [
    ["2025-05-15", "Pavilion Square / A-28-05", "Tan Wei Ming", "Lee Jia Wen", "租金收入", "2025年5月 租金", "25,000.00", "銀行轉賬", "否", "已付款", "已確認", "已同步", "1"],
    ["2025-05-14", "Pavilion Square / A-12-03", "Lim Sook May", "Ng Kai Xin", "維修費用", "空調維修 - 客廳", "1,850.00", "銀行轉賬", "是", "已付款", "待確認", "已同步", "2"],
    ["2025-05-14", "Pavilion Square / B-15-07", "Chong Soo Lin", "-", "水電費", "2025年4月水電費", "1,260.40", "FPX", "否", "已付款", "已確認", "已同步", "1"],
    ["2025-05-12", "Pavilion Square / A-28-05", "Tan Wei Ming", "Lee Jia Wen", "管理費", "2025年5月管理費", "880.00", "Direct Debit", "否", "已付款", "已確認", "已同步", "-"],
    ["2025-05-12", "Pavilion Square / C-10-02", "Wong Ah Kow", "-", "雜費", "門禁卡補辦費", "50.00", "現金", "否", "已付款", "已確認", "已同步", "-"],
    ["2025-05-11", "Pavilion Square / A-28-05", "Tan Wei Ming", "Lee Jia Wen", "維修費用", "衛生間漏水維修", "2,900.00", "銀行轉賬", "是", "部分付款", "待確認", "已同步", "2"],
    ["2025-05-10", "Pavilion Square / B-20-01", "Steven Tan", "-", "水電費", "2025年5月 電費", "860.75", "FPX", "否", "待付款", "待確認", "同步失敗", "1"],
    ["2025-05-09", "Pavilion Square / A-12-03", "Lim Sook May", "Ng Kai Xin", "雜費", "停車場月租補差", "120.00", "銀行轉賬", "否", "待付款", "未確認", "未同步", "-"],
    ["2025-05-08", "Pavilion Square / C-18-06", "Ong Mei Ling", "-", "管理費", "2025年5月管理費", "920.00", "Direct Debit", "否", "已付款", "已確認", "已同步", "-"],
    ["2025-05-07", "Pavilion Square / A-28-05", "Tan Wei Ming", "Lee Jia Wen", "租金收入", "2025年4月 租金", "25,000.00", "銀行轉賬", "否", "已付款", "已確認", "已同步", "1"]
  ]
});

Object.assign(headers, {
  adminMaintenance: ["日期", "房戶/單位", "業主", "租客", "收支類別", "收支說明", "金額", "支付方式", "預備金扣除", "付款狀態", "財務確認", "SQL 同步", "附件", "操作"]
});

Object.assign(adminKpis, {
  adminReserve: [
    ["錢", "預備金總餘額", "RM 4,385,600.00", "", "up"],
    ["戶", "低於標準業主數", "23 戶", "占比 8.62%", "down"],
    ["入", "本月累計充值", "RM 285,600.00", "↑ 18.54%", "up"],
    ["出", "本月累計扣款", "RM 112,350.00", "↓ 12.23%", "down"],
    ["待", "待財務確認", "RM 68,900.00", "共 15 筆記錄", "down"],
    ["盾", "最低標準設置數", "56 個單位", "未設置 4 個單位", "up"]
  ]
});

Object.assign(rows, {
  adminReserve: (() => {
    const base = [
      ["Tan Wei Ming", "Pavilion Square / A-28-05", "1,300,000.00", "1,200,000.00", "1,650,000.00", "350,000.00", "2026-05-22", "正常", "已確認"],
      ["李美玲", "Pavilion Square / B-12-07", "850,000.00", "1,000,000.00", "950,000.00", "100,000.00", "2026-05-20", "餘額不足", "待確認"],
      ["Ahmad Bin Ali", "Pavilion Square / C-23-01", "420,000.00", "600,000.00", "600,000.00", "180,000.00", "2026-05-21", "待充值", "待確認"],
      ["Cheah Siew Ling", "Pavilion Square / A-17-03", "1,250,000.00", "1,200,000.00", "1,300,000.00", "50,000.00", "2026-05-21", "正常", "已確認"],
      ["Lim Kok Wei", "Pavilion Square / B-08-06", "980,000.00", "1,000,000.00", "1,050,000.00", "70,000.00", "2026-05-21", "餘額不足", "待確認"],
      ["Siti Aisyah Bt Rahman", "Pavilion Square / D-10-02", "300,000.00", "500,000.00", "500,000.00", "200,000.00", "2026-05-21", "待充值", "待確認"],
      ["Wong Chun Kit", "Pavilion Square / A-30-08", "1,560,000.00", "1,200,000.00", "1,800,000.00", "240,000.00", "2026-05-21", "正常", "已確認"],
      ["Ng Mei Fen", "Pavilion Square / A-30-08", "780,000.00", "1,000,000.00", "900,000.00", "120,000.00", "2026-05-21", "餘額不足", "待確認"],
      ["Muhammad Farid", "Pavilion Square / B-21-05", "220,000.00", "600,000.00", "600,000.00", "380,000.00", "2026-05-21", "待充值", "待確認"],
      ["Elaine Chong", "Pavilion Square / A-45-01", "1,180,000.00", "1,000,000.00", "1,200,000.00", "20,000.00", "2026-05-21", "正常", "已確認"]
    ];
    const names = ["Rachel Lee", "Ho Chin Seng", "Yap Soon Huat", "Ong Mei Ling", "Nur Farah", "Steven Tan", "Chan Pui San", "Lim Jia Wei", "Lee Jia Hui", "Wong Kok Leong"];
    const units = ["C-20-08", "B-30-01", "A-19-09", "A-45-01", "C-10-02", "B-20-01", "A-33-11", "B-12-07", "B-12-03", "A-18-09"];
    while (base.length < 56) {
      const i = base.length;
      const min = i % 4 === 0 ? 1200000 : i % 4 === 1 ? 1000000 : i % 4 === 2 ? 600000 : 500000;
      const balance = i % 5 === 0 ? min - 180000 : i % 3 === 0 ? min - 50000 : min + (i % 7) * 30000;
      const topup = balance + 250000;
      const debit = topup - balance;
      base.push([
        names[i % names.length],
        `Pavilion Square / ${units[i % units.length]}`,
        moneyText(balance),
        moneyText(min),
        moneyText(topup),
        moneyText(debit),
        `2026-05-${String(22 - (i % 18)).padStart(2, "0")}`,
        balance < min ? (i % 2 ? "餘額不足" : "待充值") : "正常",
        balance < min ? "待確認" : "已確認"
      ]);
    }
    return base;
  })()
});

Object.assign(headers, {
  adminReserve: ["屋主姓名", "房戶/單位", "當前餘額", "最低標準", "累計充值", "累計扣款", "最近變動日期", "當前狀態", "財務確認", "操作"]
});

const reportTabConfigs = [
  {
    key: "propertyPayment",
    label: "房款繳費進度",
    chartTitle: "房款收款趨勢（按月）",
    splitTitle: "收款進度分布",
    unitLabel: "總單位數",
    bars: [45, 62, 72, 58, 82, 52],
    legend: [["green-dot", "已收完全", "45 (45%)"], ["gold-dot", "部分已收", "35 (35%)"], ["gray-dot", "未開始", "15 (15%)"], ["red-dot", "已逾期", "5 (5%)"]],
    fields: "單位編號、單位名稱、項目名稱、樓棟、樓層、屋主姓名、身份證號、付款計劃、應收金額、已收金額、未收金額、狀態",
    filters: ["期間：2026-01-01 ~ 2026-12-31", "項目：全部項目", "屋主：全部屋主", "狀態：全部狀態"],
    rows: [
      ["房款繳費進度", "2026-01-01 ~ 2026-12-31", "Pavilion Square", "2,580,000.00", "已完成", "2026-06-15 10:30:22"],
      ["房款繳費進度（逾期）", "2026-01-01 ~ 2026-12-31", "全部項目", "1,200,000.00", "部分完成", "2026-06-15 10:30:22"],
      ["房款待確認明細", "2026-01-01 ~ 2026-12-31", "CCPS Heights", "210,000.00", "待確認", "2026-06-15 10:28:10"]
    ]
  },
  {
    key: "rentCollection",
    label: "租金收款記錄",
    chartTitle: "租金收款趨勢（按月）",
    splitTitle: "租金收款狀態",
    unitLabel: "租約數",
    bars: [38, 55, 66, 73, 64, 70],
    legend: [["green-dot", "已收租金", "62 (58%)"], ["gold-dot", "部分收款", "24 (22%)"], ["gray-dot", "本月未到期", "12 (11%)"], ["red-dot", "逾期租金", "9 (9%)"]],
    fields: "租客資料、租約資料、每月租金、繳租日期、已收未收、收款憑證、租金收入",
    filters: ["期間：2026-01-01 ~ 2026-12-31", "項目：全部項目", "租客：全部租客", "狀態：全部狀態"],
    rows: [
      ["租金收款記錄", "2026-01-01 ~ 2026-12-31", "Pavilion Square", "1,450,000.00", "已完成", "2026-06-15 10:30:22"],
      ["未收租金提醒", "2026-06", "CCP Residence", "163,300.00", "待確認", "2026-06-15 09:45:12"],
      ["租金歷史記錄", "2026-Q2", "全部項目", "428,500.00", "已完成", "2026-06-14 17:12:30"]
    ]
  },
  {
    key: "incomeExpense",
    label: "收支明細",
    chartTitle: "收支金額趨勢（按月）",
    splitTitle: "收支類別分布",
    unitLabel: "筆數",
    bars: [50, 48, 60, 69, 74, 57],
    legend: [["green-dot", "租金收入", "48 (42%)"], ["gold-dot", "房款收入", "35 (31%)"], ["gray-dot", "管理支出", "20 (18%)"], ["red-dot", "維修支出", "10 (9%)"]],
    fields: "租金收入、維修支出、水電費、管理費、其他支出、憑證附件、財務審核狀態",
    filters: ["期間：2026-01-01 ~ 2026-12-31", "項目：全部項目", "類別：全部類別", "狀態：全部狀態"],
    rows: [
      ["收支明細", "2026-01-01 ~ 2026-12-31", "全部項目", "1,125,300.00", "已完成", "2026-06-15 10:30:22"],
      ["本月總收入", "2026-06", "Pavilion Square", "1,280,000.00", "已完成", "2026-06-15 10:20:31"],
      ["本月總支出", "2026-06", "全部項目", "254,630.00", "待確認", "2026-06-15 10:10:22"]
    ]
  },
  {
    key: "maintenance",
    label: "維修費用",
    chartTitle: "維修費用趨勢（按月）",
    splitTitle: "維修類別分布",
    unitLabel: "工單數",
    bars: [20, 32, 44, 36, 51, 47],
    legend: [["green-dot", "已付款", "34 (55%)"], ["gold-dot", "待確認", "14 (23%)"], ["gray-dot", "未付款", "9 (15%)"], ["red-dot", "同步失敗", "4 (7%)"]],
    fields: "維修費用、水電費、管理費、其他支出、支出憑證及說明、財務確認狀態",
    filters: ["期間：2026-01-01 ~ 2026-12-31", "項目：全部項目", "類別：維修費用", "狀態：全部狀態"],
    rows: [
      ["維修費用", "2026-01-01 ~ 2026-12-31", "全部項目", "185,600.00", "已完成", "2026-06-15 10:30:22"],
      ["水電費支出", "2026-06", "Pavilion Square", "38,450.00", "已完成", "2026-06-12 12:05:21"],
      ["管理費支出", "2026-06", "CCP Residence", "27,860.00", "待確認", "2026-06-11 16:22:09"]
    ]
  },
  {
    key: "reserve",
    label: "預備金流水",
    chartTitle: "預備金充值與扣款",
    splitTitle: "預備金狀態",
    unitLabel: "業主數",
    bars: [66, 58, 72, 61, 80, 75],
    legend: [["green-dot", "餘額正常", "56 (62%)"], ["gold-dot", "低於標準", "23 (25%)"], ["gray-dot", "待財務確認", "8 (9%)"], ["red-dot", "不足提醒", "4 (4%)"]],
    fields: "當前預備金餘額、充值記錄、扣款記錄、預備金不足提醒、最低標準設定",
    filters: ["期間：2026-01-01 ~ 2026-12-31", "項目：全部項目", "屋主：全部屋主", "狀態：全部狀態"],
    rows: [
      ["預備金流水", "2026-01-01 ~ 2026-12-31", "全部項目", "2,500,000.00", "已完成", "2026-06-15 10:20:31"],
      ["預備金充值記錄", "2026-06", "Pavilion Square", "285,600.00", "已完成", "2026-06-15 09:20:31"],
      ["預備金扣款記錄", "2026-06", "全部項目", "112,350.00", "待確認", "2026-06-14 18:02:55"]
    ]
  },
  {
    key: "financeConfirm",
    label: "財務確認記錄",
    chartTitle: "財務確認處理量",
    splitTitle: "確認狀態分布",
    unitLabel: "記錄數",
    bars: [42, 50, 58, 69, 62, 77],
    legend: [["green-dot", "已確認", "71 (68%)"], ["gold-dot", "部分付款", "15 (14%)"], ["gray-dot", "待確認", "12 (12%)"], ["red-dot", "未付款", "6 (6%)"]],
    fields: "待確認、部分收款、已確認收款、未付款、部分付款、已付款、已同步會計系統",
    filters: ["期間：2026-01-01 ~ 2026-12-31", "項目：全部項目", "財務：全部狀態", "狀態：全部狀態"],
    rows: [
      ["財務確認記錄", "2026-01-01 ~ 2026-12-31", "全部項目", "-", "已完成", "2026-06-15 10:30:22"],
      ["待確認收款", "2026-06", "Pavilion Square", "680,000.00", "待確認", "2026-06-15 09:12:44"],
      ["已確認收款", "2026-06", "全部項目", "1,280,000.00", "已完成", "2026-06-14 19:33:10"]
    ]
  },
  {
    key: "sqlSync",
    label: "SQL 同步記錄",
    chartTitle: "SQL 同步成功率",
    splitTitle: "同步狀態分布",
    unitLabel: "批次數",
    bars: [72, 74, 81, 76, 86, 88],
    legend: [["green-dot", "同步成功", "86 (86%)"], ["gold-dot", "同步中", "8 (8%)"], ["gray-dot", "未同步", "4 (4%)"], ["red-dot", "同步失敗", "2 (2%)"]],
    fields: "業主及客戶資料、租金帳單、房款收款記錄、分期繳費記錄、維修及其他支出、失敗原因",
    filters: ["期間：2026-01-01 ~ 2026-12-31", "項目：全部項目", "同步：全部批次", "狀態：全部狀態"],
    rows: [
      ["SQL 同步記錄", "2026-01-01 ~ 2026-12-31", "全部項目", "-", "已完成", "2026-06-15 10:30:22"],
      ["SQL 同步成功率", "2026-06", "全部項目", "98.6%", "已完成", "2026-06-15 10:30:22"],
      ["同步失敗明細", "2026-06", "CCPS Heights", "2", "待確認", "2026-06-15 09:00:00"]
    ]
  }
];

export { modules, headers, rows, featureMap, workflowMap, reminderMap, reportTabConfigs, adminKpis };
