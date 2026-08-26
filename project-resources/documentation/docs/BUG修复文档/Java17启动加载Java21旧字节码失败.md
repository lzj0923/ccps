# Java 17 启动加载 Java 21 旧字节码失败 - 修复报告

## BUG描述

- 页面/功能：CCPS 后端 Spring Boot 启动
- 操作步骤：在 `backend` 目录使用 Java 17 执行 `mvn spring-boot:run`
- 期望结果：后端正常启动并提供 `/api/health`
- 实际结果：应用上下文初始化失败，Maven 子进程以退出码 1 结束

## 复现步骤

1. 在 `target/classes` 中保留由 Java 21 编译的 `AdminReportMapper.class`。
2. 使用 Java 17 执行 `mvn spring-boot:run`。
3. Maven判断源码无需重新编译，Java 17 加载 65.0 字节码时抛出 `UnsupportedClassVersionError`。

## 根因分析

- 表象：`spring-boot-maven-plugin:3.4.5:run` 报 `Process terminated with exit code: 1`。
- 根因：`target/classes` 混入 Java 21 字节码（class major version 65），启动运行时为 Java 17，只支持到 class major version 61；Maven 增量编译未因 JDK 切换而重新编译未变更源码。
- 附加阻碍：合同模板 PDF 带有 Windows `ReadOnly` 属性，复制到 `target` 后导致 `mvn clean` 无法删除生成目录。
- 定位过程：完整日志直接指出 65.0 与 61.0 冲突；使用 `javap -verbose` 确认目标类为 65；构造字节码扫描测试后稳定检出 `AdminReportMapper` 及其内部类。

## 修复方案

- 清除 `src/main/resources/contract-templates` 及生成目录中 PDF 的 Windows 只读属性，不修改文件内容。
- 使用 Java 17 执行 `mvn clean test`，删除旧 `target` 并按 POM 的 `release 17` 全量编译 371 个主源码文件。
- 不修改业务代码。

## 单元测试

- 测试文件：`backend/src/test/java/com/ccps/backend/build/JavaBytecodeCompatibilityTest.java`
- 测试用例：`compiledMainClassesRemainJava17Compatible`
- 失败验证：在 65.0 字节码存在时，测试准确列出 `AdminReportMapper` 及其 9 个内部类。
- 修复验证：清理并以 Java 17 重编译后测试通过，目标类 major version 为 61。
- 完整套件：共运行 368 项；本次新增测试通过，另有 3 项失败和 4 项错误属于工作区既有问题，与字节码启动故障无关。

## 手动验证步骤

1. 打开 PowerShell 并进入项目的 `backend` 目录；如果提示符已经以 `backend>` 结尾，不要再次执行 `cd backend`。
2. 确认 `java -version` 显示 Java 17。
3. 首次切换 JDK 或再次遇到字节码版本错误时执行 `mvn clean spring-boot:run`。
4. 请求 `http://localhost:8080/api/health`，确认返回 HTTP 200 且 `status` 为 `UP`。

## 影响范围

- 直接影响：Java 17 本地启动和 Maven 构建产物兼容性。
- 间接影响：阻止后续构建再次无提示混入高于 Java 17 的主类字节码。
- 启动验证：Java 17 下 Spring Boot 启动成功，MySQL 连接成功，健康检查通过。
- 业务逻辑影响：无。
