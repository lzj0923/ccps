# 租赁合约房产照片替换实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让租赁合约 PDF 自动使用当前房产的物件照片替换模板示例照片。

**Architecture:** 生成请求携带 `leaseId`；后端通过租约关联的房产查询普通物件照片，PDF 服务在原模板第 20–22 页 13 个固定区域覆盖并绘制图片。照片不足时留白，不改变 PDF 页数和正文排版。

**Tech Stack:** Spring Boot、MyBatis、OpenPDF、Vue 3、Node test、Poppler。

## Global Constraints

- 只读取 `property_photos` 的普通房产照片，不读取维修、交屋或其他租约照片。
- 原始模板保持 22 页，输出格式固定为 PDF。
- 照片必须按比例填充固定区域，不得拉伸变形或覆盖页眉页码。

---

### Task 1: 写照片替换失败测试

**Files:**
- Modify: `backend/src/test/java/com/ccps/backend/service/TenancyAgreementPdfServiceTest.java`
- Test: `backend/src/test/java/com/ccps/backend/service/TenancyAgreementPdfServiceTest.java`

- [ ] **Step 1: 添加房产照片依赖的测试接口和测试数据**

让测试构造 2 张临时 JPEG，注入照片读取依赖，并调用 `generate` 时传入 `leaseId`。

- [ ] **Step 2: 增加失败断言**

断言生成 PDF 的第 20 页存在测试图片内容，并断言只有 2 张照片时剩余模板照片区域为空；在照片绘制方法尚未实现时测试应失败。

- [ ] **Step 3: 运行目标测试确认 RED**

运行：`mvn -q -f backend/pom.xml "-Dtest=TenancyAgreementPdfServiceTest" test`

预期：测试因照片读取/绘制能力尚未存在而失败。

### Task 2: 接入房产照片读取

**Files:**
- Modify: `backend/src/main/java/com/ccps/backend/mapper/AdminPropertyPhotoMapper.java`
- Modify: `backend/src/main/java/com/ccps/backend/service/AdminPropertyPhotoService.java`
- Modify: `backend/src/main/java/com/ccps/backend/dto/AdminTenancyResponse.java`
- Modify: `backend/src/main/java/com/ccps/backend/mapper/AdminTenancyMapper.java`

- [ ] **Step 1: 增加按租约读取普通物件照片的方法**

通过 `leases.unit_id -> owner_units.unit_id -> property_photos.owner_unit_id` 查询普通照片，并保留文件路径、类型、排序和封面字段。

- [ ] **Step 2: 在租约列表返回生成所需的 `leaseId`**

租约页面现有行资料带有 `leaseId`；确认生成表单将该值传给 PDF 接口，其他字段保持兼容。

- [ ] **Step 3: 保留路径安全校验**

读取照片时继续使用 `property-photos` 根目录和 `normalize/startsWith` 校验，找不到单张照片时跳过该照片。

### Task 3: 在 PDF 固定区域替换示例照片

**Files:**
- Modify: `backend/src/main/java/com/ccps/backend/service/TenancyAgreementPdfService.java`
- Modify: `backend/src/main/java/com/ccps/backend/controller/AdminContractTemplateController.java`
- Modify: `frontend/src/components/AdminTenancyWorkspace.vue`

- [ ] **Step 1: 定义 13 个固定图片区域**

使用模板坐标：第 20 页 6 个区域、第 21 页 6 个区域、第 22 页 1 个区域。

- [ ] **Step 2: 覆盖并绘制图片**

按比例缩放图片到区域，使用白色覆盖旧图；图片不足的位置只覆盖留白。

- [ ] **Step 3: 串联生成请求**

前端传 `leaseId`，后端生成服务取得该租约所属房产的普通照片，再生成 PDF。

### Task 4: 验证与视觉检查

**Files:**
- Modify: `docs/客戶問題每日紀錄.md`

- [ ] **Step 1: 运行后端目标测试**

验证页数、图片替换、照片不足留白、PDF 可打开。

- [ ] **Step 2: 运行前端测试和生产构建**

运行：`node --test tests/admin-tenancy-contract-i18n.test.mjs`；`npm exec -- vite build`。

- [ ] **Step 3: 渲染第 20–22 页**

用 Poppler 输出 PNG，检查图片比例、边界、页眉和页码没有被覆盖。

- [ ] **Step 4: 记录当天变更**

在客户问题每日记录中记录“租赁合约自动调用房产物件照片替换示例照片”。
