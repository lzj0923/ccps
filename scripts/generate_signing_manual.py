import os
from docx import Document
from docx.shared import Inches, Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH

doc = Document()
style = doc.styles['Normal']
font = style.font
font.name = 'Microsoft YaHei'
font.size = Pt(10.5)

# 标题
title = doc.add_heading('CCPS 电子签约功能操作手册', level=0)
title.alignment = WD_ALIGN_PARAGRAPH.CENTER
doc.add_paragraph('版本：1.0    日期：2026-07-25').alignment = WD_ALIGN_PARAGRAPH.CENTER
doc.add_paragraph('')

# 目录
doc.add_heading('目录', level=1)
toc = [
    '一、功能概述',
    '二、签约整体流程图',
    '三、操作步骤详解',
]
for t in toc:
    doc.add_paragraph(t)
doc.add_page_break()

# 一、功能概述
doc.add_heading('一、功能概述', level=1)
doc.add_paragraph(
    'CCPS 电子签约功能允许管理员通过系统向业主（或租客）发起在线合同签署请求。'
    '业主无需安装任何软件，通过邮件中的链接即可在手机或电脑上完成合同查看、'
    '身份验证和手写签名。签署完成后，系统自动在 PDF 合同上加盖签名和时间戳。'
)
doc.add_paragraph('适用场景：')
doc.add_paragraph('  • 租赁合同签署（Lease Contract）', style='List Bullet')
doc.add_paragraph('  • 出租委托书签署（Rental Mandate）', style='List Bullet')
doc.add_paragraph('  • 新签、续约均可使用', style='List Bullet')
doc.add_paragraph('')
doc.add_paragraph('【截图位置 - 功能入口】请截取：管理端左侧菜单中「合約管理」入口的位置')

doc.add_page_break()

# 二、流程图
doc.add_heading('二、签约整体流程图', level=1)
flow = [
    ('管理员', '上传合同PDF到系统'),
    ('管理员', '点击「發起簽署」按钮'),
    ('管理员', '填写签署人姓名、邮箱、有效期'),
    ('系统', '生成唯一签署链接 + 6位验证码'),
    ('系统', '通过SMTP邮件发送签署邀请'),
    ('业主/租客', '打开邮件 → 点击签署链接'),
    ('业主/租客', '在公开页面查看合同内容'),
    ('业主/租客', '输入邮件中的6位验证码'),
    ('业主/租客', '手写签名 → 勾选同意条款 → 提交'),
    ('系统', '验证签名数据，在PDF加盖签名+时间戳'),
    ('系统', '生成已签署合同，更新状态为 signed'),
]
for i, (role, step) in enumerate(flow, 1):
    doc.add_paragraph(f'{i}. 【{role}】 {step}')
doc.add_paragraph('')
doc.add_paragraph('【截图位置 - 整体流程图】此页留空，建议用流程图工具绘制后粘贴于此')

doc.add_page_break()

# 三、操作步骤
doc.add_heading('三、操作步骤详解', level=1)

# ---------- 步骤1 ----------
doc.add_heading('步骤1：进入合約管理页面', level=2)
doc.add_paragraph(
    '管理员登录后台后，点击左侧菜单「合約管理」（/admin/contracts），'
    '进入合同文档列表页面。该页面展示所有已上传的合同文件及其签署状态。'
)
doc.add_paragraph('合同状态说明：')
doc.add_paragraph('  • missing — 未上传合同文件', style='List Bullet')
doc.add_paragraph('  • uploaded — 合同已上传，但未发起签署', style='List Bullet')
doc.add_paragraph('  • pending — 签署请求已发起，等待签署人操作', style='List Bullet')
doc.add_paragraph('  • signed — 已完成电子签署', style='List Bullet')
doc.add_paragraph('')
p = doc.add_paragraph()
run = p.add_run('【📷 截图1】')
run.bold = True
doc.add_paragraph('截取：合約管理页面整体界面，包含左侧菜单栏和右侧合同列表')

# ---------- 步骤2 ----------
doc.add_heading('步骤2：上传合同 PDF 文件', level=2)
doc.add_paragraph(
    '在租約詳情中（/admin/tenancy），选中目标租約，点击「上傳合同」按钮，'
    '选择待签署的 PDF 合同文件上传。\n\n'
    '注意：目前电子签署仅支持 PDF 格式的合同文件（application/pdf），'
    '文件大小限制为 10MB。'
)
doc.add_paragraph('')
p = doc.add_paragraph()
run = p.add_run('【📷 截图2】')
run.bold = True
doc.add_paragraph('截取：租约详情页面上传合同的弹窗/操作区域')

# ---------- 步骤3 ----------
doc.add_heading('步骤3：发起电子签署', level=2)
doc.add_paragraph(
    '合同上传成功后，合同状态变为「已上傳」。点击合同旁的「發起簽署」按钮，'
    '弹出签署发起对话框。\n\n'
    '需要填写以下信息：\n'
    '  • 签署人姓名（Signer Name）：业主或租客的法定姓名，用于身份验证\n'
    '  • 签署人邮箱（Signer Email）：接收签署链接和验证码\n'
    '  • 签署有效期（Expiry Days）：默认 7 天，可设置 1-30 天。超期后链接自动失效\n\n'
    '点击「送出」后，系统将执行以下操作：\n'
    '  ① 生成 SHA-256 哈希的唯一 Token（签署链接标识）\n'
    '  ② 生成 6 位随机数字验证码（BCrypt 加密存储）\n'
    '  ③ 写入 electronic_signature_requests 表，状态为 pending\n'
    '  ④ 记录审计日志：start_electronic_signature'
)
doc.add_paragraph('')
p = doc.add_paragraph()
run = p.add_run('【📷 截图3】')
run.bold = True
doc.add_paragraph('截取：发起签署弹窗（包含签署人姓名、邮箱、有效期输入框）')

# ---------- 步骤4 ----------
doc.add_heading('步骤4：系统自动发送签署邮件', level=2)
doc.add_paragraph(
    '管理员提交后，系统通过配置的 SMTP 邮件服务自动向签署人发送邀请邮件。\n\n'
    '邮件格式如下：\n'
    '  邮件标题：CCPS 合約簽署邀請\n'
    '  邮件内容：\n'
    '    您好 {签署人姓名}：\n'
    '    請透過以下連結查看及簽署合約：\n'
    '    {签署链接}\n'
    '    本次驗證碼：{6位数字}（10 分鐘內有效）\n'
    '    簽署連結有效至：{截止日期时间}\n'
    '    若非本人操作，請忽略此郵件。\n\n'
    '签署链接格式：http://47.108.39.84:8080/sign/{token}\n'
    '验证码有效期：10 分钟（过期后可重发）'
)
doc.add_paragraph('')
p = doc.add_paragraph()
run = p.add_run('【📷 截图4】')
run.bold = True
doc.add_paragraph('截取：签署人收到的邮件截图（重要！需要展示完整的邮件内容）')

# ---------- 步骤5 ----------
doc.add_heading('步骤5：签署人打开链接查看合同', level=2)
doc.add_paragraph(
    '签署人在邮箱中点击签署链接后，浏览器跳转到 CCPS 公开签署页面（无需登录）。\n\n'
    '页面自动加载并展示：\n'
    '  • 合同文件名（documentName）\n'
    '  • 签署人姓名（signerName）\n'
    '  • 签署请求状态（pending / signed / expired）\n'
    '  • 合同 PDF 预览（通过 iframe 内嵌显示）\n'
    '  • 签署截止时间\n\n'
    '签署人可以在此页面查看完整合同内容。'
)
doc.add_paragraph('')
p = doc.add_paragraph()
run = p.add_run('【📷 截图5】')
run.bold = True
doc.add_paragraph('截取：公开签署页面初始状态（合同预览+签署表单区域）')

# ---------- 步骤6 ----------
doc.add_heading('步骤6：验证身份', level=2)
doc.add_paragraph(
    '签署人需要在页面表单中完成身份验证：\n\n'
    '  ① 确认签署人姓名（只读，由管理员预设）\n'
    '  ② 输入 6 位验证码（来自邮件，10 分钟有效）\n'
    '  ③ 勾选同意条款：「本人同意以電子方式簽署本合約」\n\n'
    '如果验证码错误：系统提示 "Incorrect verification code"，并记录 verification_failed 事件\n'
    '如果验证码过期：系统提示 "Verification code has expired"\n'
    '可以点击「重新發送驗證碼」按钮，系统会生成新的验证码并重新发送邮件'
)
doc.add_paragraph('')
p = doc.add_paragraph()
run = p.add_run('【📷 截图6】')
run.bold = True
doc.add_paragraph('截取：验证码输入区域，以及可能的错误提示')

# ---------- 步骤7 ----------
doc.add_heading('步骤7：手写签名并提交签署', level=2)
doc.add_paragraph(
    '验证码验证通过后，进入签名绘制阶段：\n\n'
    '  • 在签名画布区域用手指（手机触屏）或鼠标（电脑）绘制手写签名\n'
    '  • 签名使用 SVG/Cavnas 技术实现，支持平滑曲线\n'
    '  • 可以点击「清除」按钮重新绘制\n'
    '  • 确认满意后点击「提交簽署」\n\n'
    '系统处理流程：\n'
    '  ① 将手写签名转为 base64 PNG 图片数据\n'
    '  ② 使用 OpenPDF 在合同 PDF 最后一页嵌入签名图片\n'
    '  ③ 在签名位置旁写入：電子簽署：{姓名}  時間：{yyyy-MM-dd HH:mm:ss}\n'
    '  ④ 生成新的「已签署」版 PDF 文件\n'
    '  ⑤ 写入 documents 表（document_type: signed_contract）\n'
    '  ⑥ 更新 electronic_signature_requests 状态为 signed\n'
    '  ⑦ 记录事件 signed 和审计日志 complete_electronic_signature'
)
doc.add_paragraph('')
p = doc.add_paragraph()
run = p.add_run('【📷 截图7】')
run.bold = True
doc.add_paragraph('截取：手写签名绘制界面（展示画布和签名效果）')

# ---------- 步骤8 ----------
doc.add_heading('步骤8：签署完成', level=2)
doc.add_paragraph(
    '签署成功后：\n\n'
    '  签署人端：\n'
    '    • 页面自动显示签署完成状态\n'
    '    • 可以下载已签署的 PDF 合同\n\n'
    '  管理员端：\n'
    '    • 合約管理页面中合同状态更新为「signed」\n'
    '    • 可以看到新的签署记录\n'
    '    • 审计日志中可见完整操作记录\n\n'
    '  已签署 PDF 的效果：\n'
    '    • 在合同最后一页包含手写签名图片\n'
    '    • 印有签署人姓名\n'
    '    • 印有签署时间（yyyy-MM-dd HH:mm:ss，UTC+8）'
)
doc.add_paragraph('')
p = doc.add_paragraph()
run = p.add_run('【📷 截图8】')
run.bold = True
doc.add_paragraph('截取：签署完成后的页面效果 + 已签署PDF中签名和时间戳的展示')

doc.add_page_break()

# 四、技术说明
doc.add_heading('四、技术说明', level=1)
doc.add_paragraph(
    '1. 安全机制：\n'
    '    • Token 使用 SHA-256 哈希存储，原文不可逆\n'
    '    • 验证码使用 BCrypt 加密存储\n'
    '    • 签名字迹数据限制最大 1MB\n\n'
    '2. PDF 签名技术栈：OpenPDF（iText 开源分支），嵌入中文字体 STSong-Light\n\n'
    '3. 邮件配置：\n'
    '    • SMTP 服务器：spring.mail.host\n'
    '    • 发件地址：ccps.mail.from\n'
    '    • 签名前缀：SIGNING_FRONTEND_BASE（当前 http://47.108.39.84:8080）\n\n'
    '4. 数据库表：\n'
    '    • electronic_signature_requests  — 签署请求主表\n'
    '    • electronic_signature_events   — 签署事件日志\n'
    '    • electronic_signature_documents — 已签署文档\n\n'
    '5. 前端路由：\n'
    '    • /admin/contracts — 管理端合約管理\n'
    '    • /sign/{token}    — 公开签署页面（无需登录）'
)

# 保存
output = r'd:\xm\CCPS-Vue3-full-source-20260714\docs\CCPS电子签约操作手册.docx'
doc.save(output)
print(f'Word document saved to: {output}')
