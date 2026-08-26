# 后端启动因 MySQL 不兼容 DDL 退出 - 修复报告

## BUG描述

- 页面/功能：CCPS 后端启动与数据库初始化
- 操作步骤：在 `backend` 目录执行 `mvn spring-boot:run`
- 期望结果：Spring Boot 正常启动并监听 8080 端口
- 实际结果：数据库初始化失败，Maven 最终报告 Spring Boot 进程以退出码 1 终止

## 复现步骤

1. 连接项目当前配置的 MySQL 数据库。
2. 在 `backend` 目录执行 `mvn spring-boot:run -e`。
3. Spring 执行 `schema.sql` 第一条语句时抛出 `SQLSyntaxErrorException`。
4. 根异常指向 `ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS requested_transaction_date`。

## 根因分析

- 表象：Maven 显示 `spring-boot-maven-plugin:run` 退出码为 1。
- 根因：启动脚本使用了当前 MySQL 版本不支持的 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` 语法。
- 定位过程：使用 `-e` 获取完整异常链，沿 `MojoExecutionException`、`BeanCreationException`、`ScriptStatementFailedException` 定位到最终的 `SQLSyntaxErrorException` 和具体 SQL。

## 修复方案

- 修改文件：`backend/src/main/resources/schema.sql`
- 修改文件：`database/migrate_central_finance_confirmation.sql`
- 修改内容：改用 `information_schema.COLUMNS` 判断字段是否存在，再通过动态 `PREPARE / EXECUTE` 执行 `ALTER TABLE`。
- 修改原因：该写法兼容 MySQL 5.7 和 8.x，并且可重复执行。

## 单元测试

- 测试文件：`backend/src/test/java/com/ccps/backend/build/StartupSchemaMysql57CompatibilityTest.java`
- 测试用例：`startupSchemaAddsFinanceDateColumnWithoutMysql8OnlySyntax`
- 测试目标：禁止启动脚本再次出现 MySQL 8 专用语法，并验证兼容性保护语句完整存在。
- 测试结果：修复前失败，修复后通过。

## 手动验证步骤

1. 打开终端并进入 `backend` 目录。
2. 执行 `mvn spring-boot:run`。
3. 确认日志出现 `Tomcat started on port 8080`。
4. 确认日志出现 `Started CcpsBackendApplication`，且进程保持运行。

## 影响范围

- 直接影响：后端启动、中央财务确认字段的自动数据库迁移。
- 间接影响：财务确认模块依赖的 `requested_transaction_date` 字段。
- 数据影响：无数据删除或覆盖；已有字段时只执行空查询，不重复修改表结构。
- 回归测试结果：MySQL 兼容性测试、财务确认专项测试通过，真实启动验证通过。完整后端套件共运行 384 项，仍有工作区既有的 5 项失败和 2 项错误，集中在业主文件 SQL 断言、业主支出 SQL 断言、报表列断言、维修附件断言及缺失的租约 Word 模板，与本次启动脚本修改无关。
