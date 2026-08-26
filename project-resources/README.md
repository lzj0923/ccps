# 项目辅助资料

此目录统一保存不参与 CCPS 系统运行的辅助资料。系统源码、数据库迁移、部署配置和开发脚本仍保留在项目根目录对应目录中。

详细规则请查看 [项目目录与文件存放规范](documentation/项目目录与文件存放规范.md)。新增任何文件前应先按该规范判断存放位置。

## 目录说明

- `documentation/`：需求、设计、问题记录、操作手册和项目计划。
- `deliverables/`：对外交付的 Word、APK 及用户手册成品。
- `deployment/`：部署压缩包和历史数据库快照；正式迁移脚本仍位于根目录 `database/`。
- `generated/`：演示文稿、截图、下载文件、测试渲染、运行日志和临时生成物，不参与版本管理。

## 使用原则

- 系统运行资源不得放入此目录。
- 新增说明文档放入 `documentation/docs/` 的对应分类。
- 新交付成品放入 `deliverables/`。
- 可重新生成的内容统一放入 `generated/`。
- 当前部署包放入 `deployment/packages/`，数据库导出快照放入 `deployment/database-snapshots/`。
