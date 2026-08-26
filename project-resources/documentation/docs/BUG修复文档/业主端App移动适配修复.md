# 业主端 App 移动适配修复

## 问题

- 手机页面沿用桌面端固定宽度与大间距，筛选器、卡片、通知列表出现截断或比例失衡。
- Android 状态栏与页面安全区处理不完整，内容可能贴近或进入系统区域。
- 底部提示会遮住固定导航；320px 下文件日期筛选被挤出。

## 根因

1. 后置桌面样式的优先级覆盖了早期移动媒体规则，部分 `min-width` 和多列网格仍然生效。
2. 原生壳未使用 Capacitor 8 注入的 `--safe-area-inset-*` 变量统一处理安全区。
3. 缺少面向原生壳的统一字号、间距、控件高度和卡片密度令牌。

## 修复

- 为 `.capacitor-native` 增加统一的手机/平板布局覆盖，仅影响 App，不改变后台网页。
- 统一 44px 触控控件、紧凑摘要卡、单列工作区和固定五项底部导航。
- 房产、房款、财务、租金、支出维修、预备金、通知、文件页面全部补充移动规则。
- 页面禁止横向滚动；宽表格只允许在所属卡片内部横滑。
- 使用 `SystemBars` 与 CSS 安全区变量兼容 Android 边到边显示。
- 修复 320px 日期范围控件和底部提示遮挡导航。

## 验证

- 320、375、414、768 CSS 像素宽度逐页检查：页面无横向滚动，底部导航固定正常。
- 自动测试：`owner-native-responsive` 与 `owner-finance-report-content` 共 6 项通过。
- `npm run build:app`：通过。
- `npx cap sync`：Android、iOS 同步通过。
- Android `assembleDebug`：`BUILD SUCCESSFUL`。

说明：组合回归中的 `portal-auth-isolation` 仍受本地 `/api` 基础地址与测试预期地址不同影响；与本次样式及原生安全区修改无关。

## 安装包

- 文件：`deliverables/CCPS业主端-移动适配修复-20260822.apk`
- 大小：7,934,730 bytes
- SHA-256：`8132EFDB7AC20921A8CB406186C7A4504E0AA3FE2794E157698FDEDA21DED9E2`
