import fs from "node:fs/promises";
import { Presentation, PresentationFile } from "file:///C:/Users/Administrator/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/@oai/artifact-tool/dist/artifact_tool.mjs";

const OUT = "D:/xm/CCPS-Vue3-full-source-20260714/客户会议演示稿_租赁与买卖流程.pptx";
const PREVIEW_DIR = "D:/xm/CCPS-Vue3-full-source-20260714/客户会议演示稿_租赁与买卖流程_预览";
const W = 1280;
const H = 720;
const C = {
  bg: "#F6F9FB",
  white: "#FFFFFF",
  ink: "#102A43",
  muted: "#5C7185",
  rule: "#CBD5DF",
  teal: "#0B8F96",
  tealSoft: "#DDF3F3",
  blue: "#3D8DFF",
  blueSoft: "#E6F0FF",
  amber: "#D9A441",
  amberSoft: "#FFF3D8",
  green: "#2E8B57",
  gray: "#E9EFF4",
  darkGray: "#7C8E9D",
};

const deck = Presentation.create({ slideSize: { width: W, height: H } });

function addShape(slide, geometry, x, y, w, h, fill = "#00000000", line = { style: "solid", fill: "#00000000", width: 0 }, name) {
  return slide.shapes.add({ geometry, name, position: { left: x, top: y, width: w, height: h }, fill, line });
}

function addText(slide, text, x, y, w, h, opts = {}) {
  const shape = addShape(slide, "rect", x, y, w, h, opts.fill ?? "#00000000", opts.line ?? { style: "solid", fill: "#00000000", width: 0 }, opts.name);
  shape.text = text;
  shape.text.fontSize = opts.size ?? 18;
  shape.text.color = opts.color ?? C.ink;
  shape.text.bold = Boolean(opts.bold);
  shape.text.typeface = opts.typeface ?? "Microsoft YaHei";
  shape.text.alignment = opts.align ?? "left";
  shape.text.verticalAlignment = opts.valign ?? "top";
  shape.text.insets = opts.insets ?? { left: 0, right: 0, top: 0, bottom: 0 };
  return shape;
}

function addLine(slide, x, y, w, h, color = C.rule) {
  return addShape(slide, "rect", x, y, w, h, color, { style: "solid", fill: color, width: 0 });
}

function addFooter(slide, page, section = "客户会议演示版") {
  addLine(slide, 64, 680, 1152, 1, C.rule);
  addText(slide, `CCPS｜${section}`, 64, 690, 320, 20, { size: 12, color: C.muted });
  addText(slide, String(page).padStart(2, "0"), 1160, 688, 56, 20, { size: 12, color: C.muted, align: "right" });
}

function addTitle(slide, eyebrow, title, subtitle = "") {
  addText(slide, eyebrow.toUpperCase(), 64, 34, 420, 20, { size: 14, color: C.teal, bold: true });
  addText(slide, title, 64, 62, 1110, 54, { size: 36, color: C.ink, bold: true });
  if (subtitle) addText(slide, subtitle, 64, 120, 1080, 30, { size: 18, color: C.muted });
}

function addPill(slide, text, x, y, w, fill, color = C.ink) {
  addShape(slide, "roundRect", x, y, w, 28, fill, { style: "solid", fill, width: 0 });
  addText(slide, text, x + 10, y + 4, w - 20, 20, { size: 13, color, bold: true, align: "center" });
}

function addCard(slide, x, y, w, h, title, body, opts = {}) {
  addShape(slide, "rect", x, y, w, h, opts.fill ?? C.white, { style: "solid", fill: opts.line ?? C.rule, width: 1 });
  addText(slide, title, x + 20, y + 18, w - 40, 28, { size: opts.titleSize ?? 22, bold: true, color: opts.titleColor ?? C.ink });
  if (body) addText(slide, body, x + 20, y + 58, w - 40, h - 70, { size: opts.bodySize ?? 17, color: opts.bodyColor ?? C.muted });
}

function addBullets(slide, items, x, y, w, lineH = 34, opts = {}) {
  items.forEach((item, i) => {
    addShape(slide, "ellipse", x, y + i * lineH + 7, 10, 10, opts.dot ?? C.teal, { style: "solid", fill: opts.dot ?? C.teal, width: 0 });
    addText(slide, item, x + 22, y + i * lineH, w - 22, lineH, { size: opts.size ?? 17, color: opts.color ?? C.ink });
  });
}

function addScreenshot(slide, id, label, x, y, w, h, opts = {}) {
  addShape(slide, "rect", x, y, w, h, opts.fill ?? "#EDF6FA", { style: "dashed", fill: opts.line ?? C.teal, width: 2 }, `screenshot-${id}`);
  addText(slide, `截图位置 ${id}`, x + 20, y + 18, w - 40, 26, { size: 19, color: C.teal, bold: true, align: "center" });
  addText(slide, label, x + 28, y + h / 2 - 16, w - 56, 56, { size: opts.size ?? 17, color: C.muted, align: "center", valign: "middle" });
  addText(slide, "会前替换为系统截图", x + 20, y + h - 36, w - 40, 20, { size: 12, color: C.darkGray, align: "center" });
}

function addStepFlow(slide, steps, x, y, w, h, accent = C.teal) {
  const gap = 18;
  const boxW = (w - gap * (steps.length - 1)) / steps.length;
  for (let i = 0; i < steps.length - 1; i += 1) {
    addLine(slide, x + boxW * (i + 1) + gap * i + 2, y + h / 2 - 2, gap - 4, 4, C.rule);
  }
  steps.forEach((step, i) => {
    const sx = x + i * (boxW + gap);
    addShape(slide, "rect", sx, y, boxW, h, i === 0 ? C.tealSoft : C.white, { style: "solid", fill: i === 0 ? accent : C.rule, width: i === 0 ? 2 : 1 });
    addText(slide, String(i + 1).padStart(2, "0"), sx + 14, y + 14, 42, 26, { size: 16, color: i === 0 ? accent : C.darkGray, bold: true });
    addText(slide, step.title, sx + 14, y + 52, boxW - 28, 48, { size: 20, color: C.ink, bold: true, valign: "middle" });
    addText(slide, step.body, sx + 14, y + 108, boxW - 28, h - 118, { size: 15, color: C.muted });
  });
}

function addTagRow(slide, tags, x, y, maxW) {
  let cursor = x;
  tags.forEach((tag) => {
    const width = Math.max(82, tag.length * 17 + 30);
    if (cursor + width > x + maxW) return;
    addPill(slide, tag, cursor, y, width, C.gray, C.muted);
    cursor += width + 10;
  });
}

function newSlide() {
  const slide = deck.slides.add();
  slide.background.fill = C.bg;
  return slide;
}

// 1. Cover
{
  const slide = newSlide();
  addShape(slide, "rect", 0, 0, 1280, 720, C.bg, { style: "solid", fill: C.bg, width: 0 });
  addShape(slide, "rect", 0, 0, 24, 720, C.teal, { style: "solid", fill: C.teal, width: 0 });
  addText(slide, "CCPS 房产交易与租赁流程演示", 76, 132, 860, 92, { size: 52, bold: true, color: C.ink });
  addText(slide, "租赁主流程｜买卖房款流程｜文件与系统自动化", 80, 250, 820, 34, { size: 24, color: C.teal, bold: true });
  addText(slide, "帮助客户快速理解：每一步谁操作、系统做什么、最后留下哪些文件。", 80, 312, 760, 34, { size: 20, color: C.muted });
  addShape(slide, "rect", 80, 400, 510, 2, C.rule, { style: "solid", fill: C.rule, width: 0 });
  addText(slide, "客户会议版｜下周二", 80, 430, 380, 28, { size: 18, color: C.darkGray });
  addText(slide, "基于当前 CCPS 系统流程整理", 80, 468, 420, 28, { size: 17, color: C.darkGray });
  addShape(slide, "rect", 860, 100, 310, 470, C.white, { style: "solid", fill: C.rule, width: 1 });
  addText(slide, "一套流程，三类产出", 900, 150, 230, 32, { size: 24, color: C.ink, bold: true, align: "center" });
  [
    ["状态", "流程进入下一阶段", C.tealSoft, C.teal],
    ["文件", "合同、凭证、交接记录", C.blueSoft, C.blue],
    ["账单", "应收、收款、财务留痕", C.amberSoft, C.amber],
  ].forEach((row, i) => {
    const yy = 232 + i * 98;
    addShape(slide, "rect", 900, yy, 230, 72, row[2], { style: "solid", fill: row[2], width: 0 });
    addText(slide, row[0], 918, yy + 12, 68, 25, { size: 20, color: row[3], bold: true });
    addText(slide, row[1], 918, yy + 40, 190, 20, { size: 15, color: C.ink });
  });
  addText(slide, "01", 1160, 688, 56, 20, { size: 12, color: C.muted, align: "right" });
}

// 2. Executive overview
{
  const slide = newSlide();
  addTitle(slide, "先看全局", "系统把业务流程拆成：状态、文件、账单三条线", "会议中先讲主流程，再进入页面截图和细节规则。");
  addText(slide, "租赁", 78, 188, 120, 30, { size: 24, color: C.teal, bold: true });
  addLine(slide, 170, 204, 940, 3, C.teal);
  addText(slide, "房产准备", 178, 222, 120, 24, { size: 17, bold: true });
  addText(slide, "委托授权", 364, 222, 120, 24, { size: 17, bold: true });
  addText(slide, "租约签署", 560, 222, 120, 24, { size: 17, bold: true });
  addText(slide, "首期收款", 756, 222, 120, 24, { size: 17, bold: true });
  addText(slide, "持续运营", 954, 222, 120, 24, { size: 17, bold: true });
  [178, 364, 560, 756, 954].forEach((x) => addShape(slide, "ellipse", x, 198, 14, 14, C.teal, { style: "solid", fill: C.teal, width: 0 }));
  addText(slide, "买卖 / 房款", 78, 320, 170, 30, { size: 24, color: C.blue, bold: true });
  addLine(slide, 250, 336, 860, 3, C.blue);
  [258, 432, 606, 780, 954].forEach((x) => addShape(slide, "ellipse", x, 330, 14, 14, C.blue, { style: "solid", fill: C.blue, width: 0 }));
  addText(slide, "项目与单位", 258, 354, 130, 24, { size: 17, bold: true });
  addText(slide, "购房合约", 432, 354, 130, 24, { size: 17, bold: true });
  addText(slide, "付款分期", 606, 354, 130, 24, { size: 17, bold: true });
  addText(slide, "凭证与确认", 780, 354, 130, 24, { size: 17, bold: true });
  addText(slide, "交房运营", 954, 354, 130, 24, { size: 17, bold: true });
  addCard(slide, 80, 446, 330, 126, "业务角色", "业务人员｜业主/客户｜财务｜运营人员", { fill: C.white });
  addCard(slide, 452, 446, 330, 126, "系统产出", "状态变更｜合同与凭证｜应收账单｜收款记录", { fill: C.white });
  addCard(slide, 824, 446, 330, 126, "客户能看到", "操作入口清楚，文件集中，流程可追溯", { fill: C.white });
  addFooter(slide, 2);
}

// 3. Rental end-to-end
{
  const slide = newSlide();
  addTitle(slide, "租赁主流程", "一套出租业务，从房产准备走到持续运营", "每个阶段都有明确的进入条件、操作动作和文件产出。");
  addStepFlow(slide, [
    { title: "房产准备", body: "资料、交接、照片、服务费用" },
    { title: "出租委托", body: "建立委托并生成授权" },
    { title: "审核启用", body: "签署、审核、进入可出租" },
    { title: "租客租约", body: "租客、OTR、租约与合同" },
    { title: "账单收款", body: "首期、提前交租、收款" },
    { title: "运营退租", body: "月租、服务、交接、退租" },
  ], 72, 188, 1136, 210, C.teal);
  addScreenshot(slide, "R01", "租赁智控台总览 / 出租准备阶段", 72, 444, 520, 180);
  addCard(slide, 638, 444, 570, 180, "客户要记住的规则", "状态是流程主线；文件是过程证据；账单是财务结果。三条线互相关联，但不互相替代。", { fill: C.white, bodySize: 18 });
  addTagRow(slide, ["租赁委托", "授权审核", "租约合同", "租金账单", "运营交接"], 658, 566, 520);
  addFooter(slide, 3);
}

// 4. Rental preparation
{
  const slide = newSlide();
  addTitle(slide, "租赁第 1 步", "先把房产准备完整，后面才不会反复补资料", "出租准备阶段是房产进入租赁流程的起点。");
  addCard(slide, 72, 184, 410, 388, "要做什么", "", { fill: C.white });
  addBullets(slide, ["建立或核对房产基本资料", "确认业主与房产关系", "完成交房 / 接管记录与照片", "维护管理费、火险、地税等服务费用", "把房产推进到可出租状态"], 98, 250, 360, 52, { size: 18, dot: C.teal });
  addCard(slide, 510, 184, 280, 388, "系统输出", "房产档案\n出租准备状态\n交接清单与照片\n成交服务费用配置", { fill: C.tealSoft, titleColor: C.teal, bodySize: 18 });
  addScreenshot(slide, "R02", "出租准备页面\n房产资料、成交服务管理、准备状态", 824, 184, 384, 388);
  addFooter(slide, 4);
}

// 5. Mandate & authorization
{
  const slide = newSlide();
  addTitle(slide, "租赁第 2–3 步", "委托与授权是“谁可以出租、谁负责审核”的确认点", "这一步把业主意愿和后台审核记录固定下来。");
  addStepFlow(slide, [
    { title: "建立委托", body: "房产、期限、租赁条件" },
    { title: "生成授权", body: "系统形成授权委托书草稿" },
    { title: "业主签署", body: "保留签署版本与时间" },
    { title: "审核启用", body: "通过后进入出租流程" },
  ], 72, 184, 1136, 178, C.teal);
  addCard(slide, 72, 408, 520, 188, "主要文件", "租赁委托记录\n授权委托书草稿\n已签署授权文件\n审核与启用记录", { fill: C.white, bodySize: 18 });
  addScreenshot(slide, "R03", "委托授权 / 审核并启用弹窗", 638, 408, 570, 188);
  addText(slide, "建议会议确认：授权是否需要客户签名、审核人是否分角色、审核后是否允许退回修改。", 72, 620, 1136, 28, { size: 16, color: C.amber, bold: true });
  addFooter(slide, 5);
}

// 6. Tenant + contract
{
  const slide = newSlide();
  addTitle(slide, "租赁第 4–5 步", "租客资料、OTR 与租约合同，形成一条可追踪的签约链", "从租客进入，到合同生效，系统持续记录每个关键节点。");
  addCard(slide, 72, 184, 530, 388, "业务动作", "", { fill: C.white });
  addBullets(slide, ["建立或关联租客资料", "录入租期、租金、押金及付款规则", "生成 OTR / offer", "生成租赁合同与租约记录", "完成签署后进入收款与入住"], 98, 250, 470, 54, { size: 18, dot: C.blue });
  addScreenshot(slide, "R04", "租客 / 租约 / OTR 页面", 638, 184, 270, 388, { size: 16 });
  addScreenshot(slide, "R05", "合同生成与签署位置", 938, 184, 270, 388, { size: 16 });
  addText(slide, "文件链：租客记录 → OTR → Tenancy Agreement → Lease Record", 72, 610, 1136, 28, { size: 17, color: C.blue, bold: true });
  addFooter(slide, 6);
}

// 7. Rental billing & operations
{
  const slide = newSlide();
  addTitle(slide, "租赁第 6 步", "租金与服务费用各自生成账单，但都能回到同一套租赁档案", "提前交租按金额自动换算覆盖月份；服务费用支持一次性或指定月份重复收取。");
  addCard(slide, 72, 180, 350, 196, "租金账单", "输入收款金额后，系统自动计算覆盖几个月；最后一个月不足整月时，显示实际已收金额，并只生成覆盖范围内的账单。", { fill: C.tealSoft, titleColor: C.teal, bodySize: 17 });
  addCard(slide, 465, 180, 350, 196, "成交服务账单", "可选择只收一次，或指定多个月份收取；当前月份同步，后续月份按任务生成，并防止重复订单。", { fill: C.blueSoft, titleColor: C.blue, bodySize: 17 });
  addCard(slide, 858, 180, 350, 196, "运营与交接", "入住、维修、月租、收款、退租交接持续沉淀记录与照片。", { fill: C.amberSoft, titleColor: C.amber, bodySize: 17 });
  addScreenshot(slide, "R06", "确认租金收款弹窗\n金额、覆盖月份、最后一个月金额", 72, 426, 520, 182);
  addScreenshot(slide, "R07", "成交服务管理弹窗\n一次性 / 指定月份", 638, 426, 570, 182);
  addFooter(slide, 7, "客户会议演示版｜租赁重点");
}

// 8. Buying/sales end-to-end
{
  const slide = newSlide();
  addTitle(slide, "买卖 / 房款主流程", "买房业务的主线，是合约总价、付款分期与财务确认", "当前系统重点覆盖房款计划、分期、凭证与收款确认；交房后进入房产运营。");
  addStepFlow(slide, [
    { title: "项目与单位", body: "建筑、单位、产权资料" },
    { title: "购房合约", body: "成交总价与付款方" },
    { title: "付款分期", body: "到期日递增、金额对总价" },
    { title: "凭证提交", body: "客户上传每期付款凭证" },
    { title: "财务确认", body: "确认收款、录入参考号" },
    { title: "交房运营", body: "完成房产资料并进入运营" },
  ], 72, 188, 1136, 210, C.blue);
  addScreenshot(slide, "B01", "房款计划 / 付款分期页面", 72, 444, 520, 180, { line: C.blue, fill: C.blueSoft });
  addCard(slide, 638, 444, 570, 180, "买卖流程的关键控制", "付款分期合计必须等于合约总价；到期日依序递增；每一笔付款都要有凭证、确认与收据记录。", { fill: C.white, bodySize: 18 });
  addFooter(slide, 8, "客户会议演示版｜买卖重点");
}

// 9. Buying details
{
  const slide = newSlide();
  addTitle(slide, "买卖重点页面", "客户提交付款凭证，财务完成确认，系统把应收变成已收", "这里是客户最容易理解、也最适合现场演示的一段。");
  addCard(slide, 72, 184, 340, 388, "客户 / 业主侧", "", { fill: C.white });
  addBullets(slide, ["查看应付分期", "上传付款凭证", "填写付款日期与参考号", "查看已确认收款与余额"], 98, 254, 290, 62, { size: 18, dot: C.blue });
  addCard(slide, 442, 184, 340, 388, "后台 / 财务侧", "", { fill: C.white });
  addBullets(slide, ["查看房款收款与未收款", "核对金额与付款分期", "确认收款并记录收据号", "保留银行参考与凭证文件"], 468, 254, 290, 62, { size: 18, dot: C.teal });
  addScreenshot(slide, "B02", "财务确认收款 / 凭证与收据", 812, 184, 396, 388, { line: C.blue, fill: C.blueSoft, size: 16 });
  addText(slide, "买卖流程建议现场演示：先建立分期 → 再上传凭证 → 最后由财务确认。", 72, 610, 1136, 28, { size: 17, color: C.blue, bold: true });
  addFooter(slide, 9);
}

// 10. Files matrix
{
  const slide = newSlide();
  addTitle(slide, "文件清单", "文件不是附件，而是每个流程节点的结果", "下面按“阶段—动作—主要输出—确认人”整理，方便客户逐项核对。");
  const x0 = 72;
  const widths = [154, 260, 452, 270];
  const headers = ["阶段", "系统动作", "主要输出文件 / 记录", "主要确认人"];
  let x = x0;
  headers.forEach((h, i) => { addShape(slide, "rect", x, 182, widths[i], 46, C.ink, { style: "solid", fill: C.ink, width: 0 }); addText(slide, h, x + 14, 194, widths[i] - 28, 22, { size: 16, color: C.white, bold: true }); x += widths[i]; });
  const rows = [
    ["租赁｜准备", "建立房产与交接资料", "房产档案、交接清单、照片、费用配置", "业务 / 运营"],
    ["租赁｜授权", "委托、签署、审核启用", "租赁委托、授权草稿、签署文件、审核记录", "业主 / 审核人"],
    ["租赁｜签约", "建立租客、OTR、租约", "租客记录、OTR、Tenancy Agreement、Lease Record", "业务 / 租客"],
    ["租赁｜收款", "生成账单并确认收款", "租金账单、收款记录、收据、付款凭证", "财务 / 收款人"],
    ["买卖｜房款", "建立分期、上传凭证、确认收款", "购房合约、付款计划、付款凭证、收据、银行参考", "客户 / 财务"],
    ["租赁｜交接", "入住、退租、持续运营", "入住交接报告、照片、维修与费用记录、退租报告", "运营 / 业主"],
  ];
  rows.forEach((row, r) => {
    const yy = 228 + r * 62;
    let xx = x0;
    row.forEach((cell, i) => { addShape(slide, "rect", xx, yy, widths[i], 62, r % 2 === 0 ? C.white : C.gray, { style: "solid", fill: C.rule, width: 1 }); addText(slide, cell, xx + 12, yy + 12, widths[i] - 24, 40, { size: 15, color: i === 0 ? C.ink : C.muted, bold: i === 0, valign: "middle" }); xx += widths[i]; });
  });
  addFooter(slide, 10);
}

// 11. Demo path / discussion
{
  const slide = newSlide();
  addTitle(slide, "会议演示建议", "用两条主线带客户走一遍：先出租，再房款", "不需要一次讲完所有后台字段；先让客户建立完整的流程地图。");
  addCard(slide, 72, 184, 520, 382, "建议现场演示顺序", "", { fill: C.white });
  addBullets(slide, ["1｜租赁智控台：房产准备与委托授权", "2｜租客 / 租约：合同与首期账单", "3｜收款弹窗：提前交租如何换算月份", "4｜成交服务：一次性或指定月份收取", "5｜买卖房款：分期、凭证、财务确认"], 98, 252, 470, 56, { size: 18, dot: C.teal });
  addCard(slide, 638, 184, 570, 382, "建议与客户确认的规则", "", { fill: C.tealSoft, titleColor: C.teal });
  addBullets(slide, ["合同模板与签署方式", "业主 / 客户 / 财务的审批角色", "哪些服务只收一次，哪些按月份收", "账单生成时间与重复生成规则", "收据、凭证、交接报告的格式", "是否需要对接会计或银行系统"], 664, 252, 515, 46, { size: 17, dot: C.amber });
  addShape(slide, "rect", 72, 584, 1136, 70, "#F2F7FA", { style: "dashed", fill: C.teal, width: 2 }, "screenshot-C01");
  addText(slide, "截图位置 C01", 92, 596, 220, 22, { size: 15, color: C.teal, bold: true });
  addText(slide, "会后补充：客户最关心的首页或报表截图", 330, 596, 640, 22, { size: 15, color: C.muted, align: "center" });
  addText(slide, "会前替换为系统截图", 980, 596, 190, 22, { size: 12, color: C.darkGray, align: "right" });
  addFooter(slide, 11, "客户会议演示版｜下一步");
}

async function writeBlob(path, blob) {
  await fs.mkdir(path.substring(0, path.lastIndexOf("/")), { recursive: true });
  await fs.writeFile(path, Buffer.from(await blob.arrayBuffer()));
}

async function main() {
  await fs.mkdir(PREVIEW_DIR, { recursive: true });
  for (const [index, slide] of deck.slides.items.entries()) {
    const png = await deck.export({ slide, format: "png", scale: 1 });
    await writeBlob(`${PREVIEW_DIR}/slide-${String(index + 1).padStart(2, "0")}.png`, png);
  }
  const montage = await deck.export({ format: "webp", montage: true, scale: 1 });
  await writeBlob(`${PREVIEW_DIR}/montage.webp`, montage);
  const pptx = await PresentationFile.exportPptx(deck);
  await pptx.save(OUT);
  console.log(`created ${OUT}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
