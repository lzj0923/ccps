const labels = {
  signing: {
    title: ['合同签署', '合約簽署', 'Contract signing'], reminder: ['您有合同待签署', '您有合約待簽署', 'Your signature is requested'],
    pendingCount: ['待签署 {n} 份', '待簽署 {n} 份', '{n} awaiting signature'], later: ['稍后处理', '稍後處理', 'Later'], viewNow: ['立即查看', '立即查看', 'View now'],
    loading: ['正在读取签署任务', '正在讀取簽署任務', 'Loading signing requests'], empty: ['暂无签署任务', '暫無簽署任務', 'No signing requests'],
    expires: ['有效至', '有效至', 'Expires'], viewSign: ['查看并签署', '查看並簽署', 'Review & sign'], viewSigned: ['查看已签文件', '查看已簽文件', 'Signed document'],
    goSign: ['前往签署', '前往簽署', 'Sign document'], signer: ['签署人', '簽署人', 'Signer'], handwrite: ['请在下方手写签名', '請在下方手寫簽名', 'Draw your signature below'],
    clear: ['清除签名', '清除簽名', 'Clear signature'], consent: ['我已阅读完整合同，确认由本人签署并同意合同内容。', '我已閱讀完整合約，確認由本人簽署並同意合約內容。', 'I have read the full contract and agree to its contents. I confirm I am the named signer.'],
    submit: ['确认提交签署', '確認提交簽署', 'Confirm signature'], submitting: ['正在提交…', '正在提交…', 'Submitting…'],
    completeForm: ['请完成手写签名，并勾选签署确认。', '請完成手寫簽名，並勾選簽署確認。', 'Draw your signature and check the consent box.'],
    success: ['签署完成，管理端已同步。', '簽署完成，管理端已同步。', 'Signed successfully. Your manager can see the result.'],
    loadFailed: ['签署任务读取失败，请重试。', '簽署任務讀取失敗，請重試。', 'Unable to load signing requests. Please retry.'],
    submitFailed: ['签署未完成，请重试。', '簽署未完成，請重試。', 'Signature not completed. Please retry.'],
    sendApp: ['发送到业主 App', '傳送至業主 App', 'Send to owner app'], recipient: ['接收业主', '接收業主', 'Recipient owner'],
    chooseOwner: ['请选择对应的签署业主', '請選擇對應的簽署業主', 'Select the owner who will sign'], confirmSend: ['确认发起 App 签署', '確認發起 App 簽署', 'Request app signature'],
    sending: ['正在发送…', '正在傳送…', 'Sending…'], sent: ['已发送，业主打开 App 后可查看签署。', '已傳送，業主開啟 App 後可查看簽署。', 'Sent. The owner can review and sign in the app.'],
    noAccount: ['未找到关联的业主账号，请先关联账号，或继续使用签署链接。', '未找到關聯的業主帳號，請先關聯帳號，或繼續使用簽署連結。', 'No linked owner account. Link an account or use the signing link.'],
    status: { pending: ['待签署','待簽署','Pending'], signed: ['已签署','已簽署','Signed'], expired: ['已过期','已過期','Expired'], closed: ['已失效','已失效','Closed'] }
  },
  imagery: {
    architecture: ['建筑 · 空间之美', '建築 · 空間之美', 'Architecture & space'],
    living: ['家与生活', '家與生活', 'Living'],
    city: ['吉隆坡 · 城市一隅', '吉隆坡 · 城市一隅', 'A glimpse of Kuala Lumpur'],
    home: ['让日常，多一点从容', '讓日常，多一點從容', 'Room for everyday living']
  },
  redesign: {
    welcome: ['你好，{name}', '你好，{name}', 'Hello, {name}'],
    overview: ['我的资产速览', '我的資產速覽', 'Portfolio at a glance'],
    assetValue: ['资产总价值', '資產總價值', 'Total asset value'],
    searchAssets: ['搜索房产', '搜尋房產', 'Search properties'],
    rentIncome: ['本月租金收入', '本月租金收入', 'Monthly rent income'],
    rentalOverview: ['本月租务概况', '本月租務概況', 'Monthly rental overview'],
    photoUnavailable: ['暂无照片', '暫無照片', 'No photo']
  },
  portfolioHeading: ['{month} · 资产总价值', '{month} · 資產總價值', '{month} · Total asset value'],
  portfolioOwner: ['{name}名下房产', '{name}名下房產', 'Properties owned by {name}'],
  shortcuts: {
    assets: ['全部资产', '全部資產', 'Assets'], rentals: ['租务总览', '租務總覽', 'Rentals'], notices: ['重要消息', '重要消息', 'Messages'],
    documents: ['合同凭证', '合約憑證', 'Documents'], cashflow: ['房产收支', '房產收支', 'Cashflow'], tenants: ['租客资讯', '租客資訊', 'Tenants'], reserve: ['预备金', '預備金', 'Reserve']
  },
  navigation: { myProperties: ['首页', '首頁', 'Home'], ownerNotice: ['消息', '消息', 'Inbox'], ownerProjects: ['资产', '資產', 'Assets'], ownerRentalHub: ['租务', '租務', 'Rentals'], ownerMore: ['我的', '我的', 'Me'] },
  coverMissing: ['暂无可显示的房产照片', '暫無可顯示的房產照片', 'Property photo unavailable'],
  paymentNotArchived: ['暂无分期记录，已缴及待缴金额请联系管理人员核实。', '暫無分期記錄，已繳及待繳金額請聯絡管理人員核實。', 'No installment records. Contact your manager to verify paid and outstanding amounts.'],
  paymentIncomplete: ['分期资料未齐', '分期資料未齊', 'Incomplete payment records'],
  projectsUnavailable: ['建案资讯尚未接入', '建案資訊尚未接入', 'Project announcements are not connected'],
  projectsUnavailableHint: ['此系统尚未配置面向业主的建案发布和讲座报名服务，需联系服务人员了解。', '此系統尚未配置面向業主的建案發布和講座報名服務，需聯絡服務人員了解。', 'Owner project announcements and seminar registration are not configured. Contact your service team.'],
  occupied: ['出租中', '出租中', 'Occupied'], managedCount: ['{count} 套租管房产', '{count} 套租管房產', '{count} rental-service properties'],
  account: {
    title: ['账号与联系资料', '帳號與聯絡資料', 'Account and contact details'], edit: ['编辑联系资料', '編輯聯絡資料', 'Edit contact details'],
    scope: ['可修改联系资料；不影响登录账号、法定姓名及合同资料。', '可修改聯絡資料；不影響登入帳號、法定姓名及合約資料。', 'Contact changes do not change your login, legal name or contracts.'],
    mobilePhone: ['联系手机', '聯絡手機', 'Contact mobile'], homePhone: ['住宅电话', '住宅電話', 'Home phone'], officePhone: ['办公电话', '辦公電話', 'Office phone'], mailingAddress: ['通讯地址', '通訊地址', 'Mailing address'],
    save: ['保存资料', '儲存資料', 'Save details'], cancel: ['取消', '取消', 'Cancel'], saved: ['联系资料已保存', '聯絡資料已儲存', 'Contact details saved'],
    password: ['修改密码', '修改密碼', 'Change password'], currentPassword: ['当前密码', '目前密碼', 'Current password'], newPassword: ['新密码', '新密碼', 'New password'], confirmPassword: ['确认新密码', '確認新密碼', 'Confirm new password'],
    passwordHint: ['至少10位，包含字母和数字。修改后需重新登录所有设备。', '至少10位，包含字母和數字。修改後需重新登入所有裝置。', 'Use at least 10 characters with letters and numbers. All devices must sign in again.'],
    mismatch: ['两次新密码不一致', '兩次新密碼不一致', 'New passwords do not match'], signInAgain: ['密码已修改，请重新登录', '密碼已修改，請重新登入', 'Password changed. Please sign in again.']
  },
  proof: {
    reserve: ['预备金充值凭证', '預備金充值憑證', 'Reserve top-up proof'], payment: ['房款付款凭证', '房款付款憑證', 'Property payment proof'],
    hint: ['提交已完成的银行转账凭证，财务审核后才会更新账目。这里不会直接扣款。', '提交已完成的銀行轉帳憑證，財務審核後才會更新帳目。這裡不會直接扣款。', 'Submit proof of a completed bank transfer. Balances change only after finance approval. No payment is taken here.'],
    open: ['填写并上传凭证', '填寫並上傳憑證', 'Upload payment proof'], target: ['对应房产／分期', '對應房產／分期', 'Property / installment'], choose: ['请选择', '請選擇', 'Select'],
    paymentDate: ['实际付款日期', '實際付款日期', 'Actual payment date'], bankName: ['付款银行', '付款銀行', 'Paying bank'], reference: ['转账参考编号', '轉帳參考編號', 'Transfer reference'], payerName: ['付款人姓名', '付款人姓名', 'Payer name'], note: ['备注', '備註', 'Note'], files: ['凭证附件', '憑證附件', 'Proof files'],
    fileHint: ['1至4份 JPG、PNG 或 PDF，每份不超过10MB。', '1至4份 JPG、PNG 或 PDF，每份不超過10MB。', '1–4 JPG, PNG or PDF files, up to 10 MB each.'], submit: ['提交财务审核', '提交財務審核', 'Submit for finance review'], submitted: ['已提交，等待财务审核。编号：', '已提交，等待財務審核。編號：', 'Submitted for finance review. Reference:'], noTarget: ['此房产暂无可提交的分期或预备金账户，请联系管理员。', '此房產暫無可提交的分期或預備金帳戶，請聯絡管理員。', 'No eligible installment or reserve account for this property. Contact your manager.'],
    errors: { target: ['请选择对应项目', '請選擇對應項目', 'Select a target'], amount: ['金额须大于0，不超过可提交金额，最多两位小数', '金額須大於0，不超過可提交金額，最多兩位小數', 'Enter a positive amount within the limit, with at most two decimals'], date: ['请选择有效付款日期，不能晚于今天', '請選擇有效付款日期，不能晚於今天', 'Select a payment date no later than today'], required: ['请填写银行、参考编号和付款人', '請填寫銀行、參考編號和付款人', 'Complete bank, reference and payer'], length: ['填写内容过长，请缩短', '填寫內容過長，請縮短', 'Some fields are too long'], files: ['请上传1至4份有效的JPG、PNG或PDF，每份不超过10MB', '請上傳1至4份有效的JPG、PNG或PDF，每份不超過10MB', 'Upload 1–4 valid JPG, PNG or PDF files, up to 10 MB each'] }
  },
  selfServiceHint: ['联系资料可自行维护，付款凭证提交后需财务审核；资产归属、合同及已确认账目由管理人员维护。', '聯絡資料可自行維護，付款憑證提交後需財務審核；資產歸屬、合約及已確認帳目由管理人員維護。', 'Edit your contact details and submit payment proofs for review. Ownership, contracts and confirmed accounts remain managed by staff.'],
  fileCount: ['显示 {count} / {total} 份资料', '顯示 {count} / {total} 份資料', 'Showing {count} of {total} files'],
  documentCategories: {
    all: ['全部文件', '全部文件', 'All files'], sale: ['买卖合同', '買賣合約', 'Purchase contracts'], lease: ['租赁合同', '租賃合約', 'Lease contracts'],
    property_contract: ['房产授权与合同', '房產授權與合約', 'Property agreements'], proof: ['收据凭证', '收據憑證', 'Receipts and payment proofs'],
    cashflow: ['收支与维修附件', '收支與維修附件', 'Cashflow and maintenance'], handover: ['交接资料', '交接資料', 'Handover records'], photo: ['房屋照片', '房屋照片', 'Property photos'], other: ['其他资料', '其他資料', 'Other files']
  },
  property: ['房产', '房產', 'Property'], payment: ['房款', '房款', 'Payments'], cashflow: ['收支', '收支', 'Cashflow'], tenant: ['租客', '租客', 'Tenants'], files: ['文件', '文件', 'Files'], facts: ['资料', '資料', 'Details'],
  search: ['搜索项目、房号或租客', '搜尋項目、房號或租客', 'Search project, unit or tenant'], all: ['全部', '全部', 'All'], searchRecords: ['搜索交易摘要', '搜尋交易摘要', 'Search transactions'],
  month: ['月份', '月份', 'Month'], category: ['费用分类', '費用分類', 'Category'], direction: ['收支类型', '收支類型', 'Direction'], reset: ['重置筛选', '重設篩選', 'Reset filters'],
  income: ['收入', '收入', 'Income'], expense: ['支出', '支出', 'Expense'], net: ['净收支', '淨收支', 'Net cashflow'], annual: ['年度净收支', '年度淨收支', 'Annual net cashflow'],
  balance: ['交易后结余', '交易後結餘', 'Balance after transaction'], balanceHint: ['结余包含历年记录，不随筛选重新计算。', '結餘包含歷年記錄，不隨篩選重新計算。', 'Balances include previous years and do not change with filters.'],
  reserve: ['预备金余额', '預備金餘額', 'Reserve balance'], deposit: ['租客押金', '租客押金', 'Tenant deposit'], monthlyNet: ['本月净收支', '本月淨收支', 'Monthly net cashflow'], leaseEnd: ['合约到期', '合約到期', 'Lease ends'],
  attachments: ['查看附件', '查看附件', 'Attachments'], attachmentNumber: ['附件 {n}', '附件 {n}', 'File {n}'], noAttachment: ['暂无附件', '暫無附件', 'No attachments'], proofUnavailable: ['本期凭证尚未关联，请联系管理员核实。', '本期憑證尚未關聯，請聯絡管理員核實。', 'No proof is linked to this installment. Contact your manager.'],
  photos: ['房屋照片', '房屋照片', 'Property photos'], before: ['出租前', '出租前', 'Before rental'], after: ['退租后', '退租後', 'After rental'], current: ['房屋现况', '房屋現況', 'Current condition'], allLeases: ['全部租约', '全部租約', 'All leases'],
  noPhotos: ['此租约或阶段暂无照片', '此租約或階段暫無照片', 'No photos for this lease or stage'], photoFailed: ['缩略图加载失败', '縮圖載入失敗', 'Thumbnail unavailable'], retry: ['重试', '重試', 'Retry'],
  photoHint: ['选择同一租约，分别查看出租前和退租后的记录。', '選擇同一租約，分別查看出租前和退租後的記錄。', 'Select a lease to compare its before and after photos.'],
  notice: ['消息详情', '訊息詳情', 'Message details'], close: ['关闭', '關閉', 'Close'], relatedProperty: ['查看相关房产', '查看相關房產', 'View property'], unreadOnly: ['只看未读', '只看未讀', 'Unread only'], searchNotices: ['搜索消息或房产', '搜尋訊息或房產', 'Search messages or properties'],
  searchFiles: ['搜索文件名称', '搜尋文件名稱', 'Search file names'], selectionNet: ['筛选结果净收支', '篩選結果淨收支', 'Filtered net cashflow'], refresh: ['刷新', '重新整理', 'Refresh'], noMatches: ['没有符合筛选条件的房产', '沒有符合篩選條件的房產', 'No matching properties'],
  currentLease: ['当前租约', '目前租約', 'Current lease'], noCurrentLease: ['暂无当前租约', '暫無目前租約', 'No current lease'], contract: ['查看合同', '查看合約', 'View contract'],
  date: ['日期', '日期', 'Date'], description: ['摘要', '摘要', 'Description'], currency: ['币种', '幣別', 'Currency'], amount: ['金额', '金額', 'Amount'], exportFailed: ['导出失败，请重试', '匯出失敗，請重試', 'Export failed. Please retry.'],
  categories: {
    deposit: ['租客押金', '租客押金', 'Tenant deposit'], income: ['其他收入', '其他收入', 'Other income'],
    rent: ['租金收入', '租金收入', 'Rent'], management_fee: ['代管费', '代管費', 'Management service'], management: ['物业管理费', '物業管理費', 'Building management'], tax: ['消费税 / VAT', '消費稅 / VAT', 'Tax / VAT'], maintenance: ['维修支出', '維修支出', 'Maintenance'], refund: ['业主结付 / 退款', '業主結付 / 退款', 'Remittance / refund'], reserve: ['预备金异动', '預備金異動', 'Reserve movement'], expense: ['其他支出', '其他支出', 'Other expenses']
  }
};
function translate(value, index) {
  return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, Array.isArray(item) ? item[index] : translate(item, index)]));
}
export const ownerAppMessages = Object.fromEntries(['zh-CN', 'zh-TW', 'en'].map((locale, index) => [locale, translate(labels, index)]));
