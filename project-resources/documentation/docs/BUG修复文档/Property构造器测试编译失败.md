# Property 构造器测试编译失败 - 修复报告

## BUG描述
- 页面/功能：后端 Maven 测试编译
- 操作步骤：在 `backend` 目录执行 `mvn test`
- 期望结果：测试源码成功编译并进入测试执行阶段
- 实际结果：`AdminPropertyWorkspaceServiceTest` 无法调用 `AdminOwnerResponse.Property` 的最新构造器，`testCompile` 失败

## 复现步骤
1. 检查 `AdminPropertyWorkspaceServiceTest` 第 41 行的 `Property` 测试夹具。
2. 对照 `AdminOwnerResponse.Property` 当前记录定义。
3. 确认测试夹具仅传入 25 个参数，而记录构造器需要 32 个参数。

## 根因分析
- 表象：Maven 编译器报告 `Property` 实际参数列表和形式参数列表长度不同。
- 根因：`Property` 新增了 7 个水电及税务账号字段后，工作区服务测试的构造数据没有同步补齐这些字段。
- 定位过程：逐项比对记录定义与唯一直接构造该 DTO 的测试代码，确认缺少的字段均位于 `paymentStatus` 之后。

## 修复方案
- 修改文件：`backend/src/test/java/com/ccps/backend/service/AdminPropertyWorkspaceServiceTest.java`
- 修改内容：在测试夹具末尾补齐 7 个可空账号字段。
- 修改原因：该测试场景不依赖账号数据，使用 `null` 能保持原测试语义，同时匹配最新 DTO 契约。

## 单元测试
- 测试文件：`AdminPropertyWorkspaceServiceTest.java`
- 测试用例：`assemblesAuthoritativeModulesAndDetectsMissingLeaseContract`
- 定向验证：连同业主报告权限测试共运行 4 项，全部通过；主源码 350 个、测试源码 73 个均已由 JDK 17 重新编译成功。
- 全量回归：共运行 329 项；本次修复相关测试通过，另有 1 项既有 SQL 文本断言失败及 2 项租约 Word 模板资源缺失错误，与本次构造器修复无关。

## 手动验证步骤
1. 设置 `JAVA_HOME` 为项目使用的 JDK 17。
2. 进入 `backend` 目录。
3. 执行 `mvn -Dtest=AdminPropertyWorkspaceServiceTest test`。
4. 确认 `testCompile` 成功，且该测试显示 `Failures: 0, Errors: 0`。

## 影响范围
- 直接影响：后端测试源码可以再次编译，工作区聚合测试可以执行。
- 间接影响：无；未修改生产业务代码或 DTO 定义。
- 回归测试结果：相关测试通过；全量套件仍存在 3 项无关既有失败，需另行处理。
