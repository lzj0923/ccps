# CCPS 项目协作规则

- 始终使用简体中文沟通，保持专业、简洁。
- 创建或移动文件前，必须先阅读 `project-resources/documentation/项目目录与文件存放规范.md`。
- 项目根目录不得新增散落的文档、截图、日志、压缩包、数据库快照或临时目录。
- 系统源码只放在 `backend/`、`frontend/`；正式数据库脚本只放在 `database/`。
- 文档、交付物、部署快照和可重新生成文件必须放入 `project-resources/` 对应分类。
- 测试渲染、调试截图、一次性脚本和日志统一放入 `project-resources/generated/`。
- 前端标准构建目录 `frontend/dist/`、后端标准构建目录 `backend/target/` 除外，但不得手工修改其中内容。
- 不得删除、覆盖或移动用户已有文件，除非任务明确要求；整理时必须保留内容并同步更新引用。
