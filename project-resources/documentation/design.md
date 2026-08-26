# Design — CCPS 屋主端 App

统一设计系统。后续业主端页面沿用本文件；`frontend/tokens.css` 是令牌真源。

## System
- Genre · modern-minimal
- Macrostructure · Workbench
- Theme · existing CCPS · cool paper / teal signal / navy ink
- Audience · 查看房产、房款、财务、通知及文件的业主
- Mobile stance · 紧凑工作台，信息优先，单手操作

## Layout
- 320–639px · 单列主内容；摘要卡两列；筛选器一至两列；底部五项导航
- 640–820px · 摘要按信息密度使用两至四列；详情仍保持单列，避免横向溢出
- 821px+ · 保留现有桌面布局和顶部导航
- 表格 · 手机端限定在自身容器横滑，不允许页面横向滚动
- 安全区 · 使用 Capacitor 8 `--safe-area-inset-*`，兼容 Android 16 边到边

## Tokens
```css
:root {
  --color-native-canvas: oklch(97% .007 235);
  --color-native-surface: oklch(100% .003 215);
  --color-native-ink: oklch(29% .035 232);
  --color-native-accent: oklch(47% .095 196);
  --color-native-accent-ink: oklch(98% .006 205);
  --color-native-focus: oklch(43% .12 197);
  --font-native-display: "HarmonyOS Sans SC", "Noto Sans SC", sans-serif;
  --font-native-body: "Noto Sans SC", "Microsoft YaHei", sans-serif;
  --native-control-height: 2.75rem;
  --radius-native-card: .75rem;
  --radius-native-control: .625rem;
}
```

## Components
- App bar · 品牌标志、语言、账号入口；内容不与状态栏重叠
- Hero · 只承载当前模块标题与一句说明，高度约 92px
- Summary · 两列紧凑数据卡，数值使用等宽数字
- Filters · 44px 控件；搜索全宽；操作按钮不换行
- Records · 手机卡片化；次要字段弱化；详情操作保持可见
- Bottom tabs · 五项固定导航；44px 触控目标；文字单行省略

## States
- Focus · 2px teal ring，立即出现
- Pressed · 1px 下压，120ms
- Disabled · 55% opacity + 禁用语义
- Loading · 保留原标签或结构化骨架
- Error / success · 颜色同时配文字或图标，不只依赖颜色

## Motion
- 仅保留按钮按压、页签状态和必要加载反馈
- Reduced motion · 最长 150ms，仅透明度或静态状态切换

## Exports
- CSS · `frontend/tokens.css`
- Tailwind v4 · 将同名令牌映射到 `@theme`
- DTCG · `color.native.*`、`space.native.*`、`size.native.*`
- shadcn/ui · accent → primary，canvas → background，ink → foreground

## Guardrails
- 禁止页面级横向滚动、固定桌面最小宽度、两行按钮文字和小于 44px 的触控目标
- 不改变路由、接口字段、权限、翻译键及数据流程
