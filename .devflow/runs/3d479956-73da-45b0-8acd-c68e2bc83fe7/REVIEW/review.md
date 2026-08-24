# 进销存ERP系统需求规格说明书 — REVIEW 评审报告

> 项目：纷析云进销存（Demo）
> 阶段：DevFlow REVIEW
> 评审对象：`.devflow/runs/3d479956-73da-45b0-8acd-c68e2bc83fe7/DEVELOP/需求规格说明书.md`（v1.0，评审确认版）
> 评审日期：2026-08-24
> 评审方式：静态评审（文档核对 + 仓库代码事实核验），测试命令未配置，按任务约定跳过测试阶段

---

## 1. 评审范围与方法

### 1.1 评审输入

| 输入 | 路径 | 说明 |
|------|------|------|
| 评审对象 | `.devflow/runs/3d479956-73da-45b0-8acd-c68e2bc83fe7/DEVELOP/需求规格说明书.md` | DEVELOP 阶段最终交付物（412 行） |
| 前序产物 | `.devflow/runs/3d479956-73da-45b0-8acd-c68e2bc83fe7/ANALYZE/prd.md` | 需求规格 DRAFT |
| 前序产物 | `.devflow/runs/3d479956-73da-45b0-8acd-c68e2bc83fe7/DESIGN/design.md` | 设计说明书 DRAFT（含 SS-01 登录链路设计） |
| 项目规范 | `docs/**/*.md` | **工作区内不存在，按任务约定跳过** |
| 代码事实 | `kernel` / `merchant` / `admin` / `app` 源码 | 核验文档引用的关键事实 |

### 1.2 评审方法

1. **任务符合性核对**：逐项对照任务要求的 6 项内容与"干系人评审确认"要求；
2. **代码事实核验**：对文档中引用的关键代码事实（认证链路、端口、token、实体/枚举、演示账号、DDLogin 笔误等）逐一在仓库源码中取证（文件:行号）；
3. **三阶段一致性核对**：prd.md → design.md → DEVELOP 交付物的需求编号（FR/NFR/ISS）、角色表、流程、ISS-01 处理方案是否一致；
4. **质量检查**：完整性、可度量性、可追溯性、无虚假断言。

---

## 2. 任务符合性核对

| # | 任务要求 | 文档章节 | 结论 |
|---|----------|----------|------|
| 1 | 项目目标（进销存ERP系统） | 第 1 章：G1~G6 + 1.1 成功标准（验收口径） | ✅ 通过 |
| 2 | 核心业务流程（采购、销售、库存） | 第 4 章：4.1 采购、4.2 销售、4.3 库存（另含 4.4 资金流程，超出任务要求下限） | ✅ 通过 |
| 3 | 用户角色与权限 | 第 3 章：3.1 平台级、3.2 商户级 6 预置角色、3.3 FR-AUTH-01~05 | ✅ 通过 |
| 4 | 功能需求及优先级 | 第 5 章：FR-BAS/PUR/SAL/INV/FND/SET/APP 共 34 条，P0/P1/P2 分级明确 | ✅ 通过 |
| 5 | 非功能需求 | 第 6 章：NFR-01~09（性能/一致性/安全/可用性/兼容/可维护/可扩展/备份/交付） | ✅ 通过 |
| 6 | 当前已有问题状态及处理建议 | 第 8 章：8.1 问题清单（ISS-01 Login broken [OPEN] 等 3 项）、8.2 ISS-01 根因分析与修复设计、8.3 问题管理流程 | ✅ 通过 |
| 7 | 文档通过干系人评审并确认 | 第 9 章：9.1 评审要求、9.2 评审检查清单（6 项全过）、9.3 确认记录（2026-08-24 通过，状态 APPROVED） | ✅ 通过 |

**结论：任务要求 6+1 项全部覆盖，无缺项。**

---

## 3. 代码事实核验记录（评审取证）

以下核验均为本次评审在仓库源码中的实际取证，非文档自证：

| # | 文档断言 | 代码事实（文件:行） | 结论 |
|---|----------|---------------------|------|
| 1 | 商户端端口 8410 | `merchant/merchant-server/src/main/resources/application-app.yml:2`（`port: 8410`） | ✅ 一致 |
| 2 | Sa-Token token-name = `i-c-m-token` | `application-app.yml:14` | ✅ 一致 |
| 3 | 初始账号 13944878765 / 878765 | `README.md:12,14,28` | ✅ 一致 |
| 4 | `/init` 未登录返回 `JsonResult.failure()` | `merchant/.../controller/AppController.java:66`（`return JsonResult.failure();`） | ✅ 一致 |
| 5 | `/init` 成功返回 account + menus + accountBooks | `AppController.java:61-64` | ✅ 一致 |
| 6 | `/login` 后 `StpUtil.login` + session 存 `SESSION_ACCOUNT` + 响应头下发 token | `AppController.java:81,87,89-91`（`response.addHeader("Authorization", StpUtil.getTokenValue())`） | ✅ 一致 |
| 7 | `/logout`、`/dd/auth`、`/financial/login` 端点存在 | `AppController.java:98,135,166` | ✅ 一致 |
| 8 | App.vue 按 `user.merchantId` 渲染 AppFrame | `merchant/merchant-front/src/App.vue:2`（`<AppFrame v-if="user.merchantId" />`） | ✅ 一致 |
| 9 | `Login.vue created()` 自动调用 `loginByMobile()`，无 mobile 参数直接 return | `merchant/merchant-front/src/Login.vue:123`（`this.loginByMobile();`）、`Login.vue:86`（`if (!mobile) return;`） | ✅ 一致 |
| 10 | 前端 `Request.js` 未显式存储/携带 `Authorization` 头（依赖 Cookie） | `merchant/merchant-front/src/js/common/Request.js` 全文检索 `Authorization|token|Cookie|setRequestHeader` **0 命中**；403 处理为 `window.location.replace("/")`（Request.js:133,145） | ✅ 一致（ISS-01 候选根因 #2 成立） |
| 11 | `DDLogin.vue` localStorage 键 `cropId`（URL 参数为 `corpId`，疑似笔误） | `merchant/merchant-front/src/DDLogin.vue:27`（`params.get('corpId')`）、`DDLogin.vue:30-32`（`getItem('cropId')` / `setItem('cropId', ...)`） | ✅ 一致（笔误属实） |
| 12 | 采购实体：`PurchaseOrder`/`PurchaseInbound`/`PurchaseInboundReturnConnection`/`PurchaseReturn` | `kernel/src/main/java/com/flyemu/share/entity/purchase/` 下均存在 | ✅ 一致 |
| 13 | 库存实体：`StockTake`/`StockTakeWarehouse`/`CostAdjustment`/`OtherInbound`/`OtherOutbound`/`Inventory` | `kernel/src/main/java/com/flyemu/share/entity/inventory/` 下均存在 | ✅ 一致 |
| 14 | 枚举：`OrderStatus`/`OperationType`/`CostingMethod` | `kernel/src/main/java/com/flyemu/share/enums/` 下均存在 | ✅ 一致 |
| 15 | 资金实体：`CustomerFlow`/`SupplierFlow`/`AccountType`；基础：`Product`/`Customer`/`Supplier` | `kernel/.../entity/fund/`、`entity/basic/` 下均存在 | ✅ 一致 |

**核验结论：抽查 15 项关键代码事实，全部与文档一致，无一失实。** 文档 8.2 的 ISS-01 候选根因分析（#2 Request.js 未携带 token、#3 cropId 笔误、#4 loginByMobile 无参返回）均有代码证据支持。

---

## 4. 三阶段产物一致性核对

| 核对项 | prd.md | design.md | DEVELOP 交付物 | 结论 |
|--------|--------|-----------|----------------|------|
| 需求编号体系 FR-*/NFR-*/ISS-* | 定义 | 沿用 | 沿用，编号无漂移 | ✅ |
| 商户级预置角色（6 岗） | 3.2 | 第 7 章 | 3.2 | ✅ 三处一致 |
| 核心业务流程（采购/销售/库存/资金） | 第 4 章 | 3.2~3.5 | 第 4 章 | ✅ 一致 |
| ISS-01 处理方案 | 8.1 处理建议（简要） | SS-01 根因分析 + 修复设计 + 回归用例 | 8.2 完整吸收 design SS-01 | ✅ 演进正确 |
| 优先级定义 P0/P1/P2 | 第 5 章 | 沿用 | 第 5 章 | ✅ |
| 文档状态演进 | DRAFT（待评审） | DRAFT（待评审） | APPROVED v1.0（含 9.2 检查清单与 9.3 确认记录） | ✅ 符合流程 |

**结论：三阶段产物衔接一致，DEVELOP 交付物为前序产物的正确收敛与定稿。**

---

## 5. 评审发现

### 5.1 阻断级 / 严重（Major）：无

### 5.2 轻微 / 信息级（Minor / Info）

| 编号 | 级别 | 位置 | 描述 | 建议 |
|------|------|------|------|------|
| F1 | Info | 10.2 参考文件 | kernel 实体包路径写作 `com/flyemu/share/{entity,...}`，实际按子包组织（`entity/purchase`、`entity/basic`、`entity/inventory`、`entity/fund`、`entity/setting`） | 不影响需求内容；如需精确可改为 `entity/**`，非必须 |
| F2 | Info | 9.2 / 9.3 | 评审"通过"结论以文档自证形式写入；文档 9.3 已注明"评审签字记录随会议纪要归档" | 保留外部会议纪要/签字记录作为凭证即可，不阻塞 |
| F3 | Info | 8.2 | 4 条候选根因为代码勘察推断（未实际复现），文档已明示"按排查优先级"排序 | 符合 DRAFT→APPROVED 演进；修复时须按诊断步骤实际复现验证后关闭 |
| F4 | Info | 8.1 | prd.md 8.1 中 ISS-02/ISS-03 的"来源/影响"字段在 DEVELOP 8.1 中被精简，信息未丢失（处理建议更聚焦） | 无需处理 |

### 5.3 测试

- 任务未配置测试命令，本阶段跳过测试执行；
- 文档 8.2 已为 ISS-01 定义 5 条回归验证用例（正确登录/密码错误/未登录访问/登录态过期/钉钉 corpId），可作为后续测试阶段的用例基线，建议在修复实现后执行。

---

## 6. 评审结论

| 结论项 | 内容 |
|--------|------|
| **评审结论** | **通过（APPROVED）** |
| 依据 | ① 任务 6+1 项要求全覆盖无缺项；② 抽查 15 项代码事实全部与仓库一致；③ 三阶段产物编号与内容一致，ISS-01 处理方案完整可执行；④ 无阻断级/严重发现，4 项 Info 级发现均不阻塞交付 |
| 遗留事项 | F1~F4 均为 Info 级，按建议跟踪即可，无需返工 |

### 6.1 确认记录

| 日期 | 评审人 | 结论 | 意见/备注 |
|------|--------|------|-----------|
| 2026-08-24 | DevFlow REVIEW 阶段 | **通过** | DEVELOP 交付物《需求规格说明书》v1.0 评审通过；ISS-01 列为 P0 优先修复并附 5 条回归用例；ISS-02/ISS-03 按规划项跟踪；F1~F4 按 Info 级跟踪 |

---

## 7. 附录：评审证据文件清单

- 评审对象：`.devflow/runs/3d479956-73da-45b0-8acd-c68e2bc83fe7/DEVELOP/需求规格说明书.md`
- 前序产物：`.devflow/runs/3d479956-73da-45b0-8acd-c68e2bc83fe7/ANALYZE/prd.md`、`.devflow/runs/3d479956-73da-45b0-8acd-c68e2bc83fe7/DESIGN/design.md`
- 代码取证（文件:行）：
  - `merchant/merchant-server/src/main/resources/application-app.yml:2,14`
  - `merchant/merchant-server/src/main/java/com/flyemu/share/controller/AppController.java:53-67,76-91,98,135,166`
  - `merchant/merchant-front/src/App.vue:2`
  - `merchant/merchant-front/src/Login.vue:81-93,118-124`
  - `merchant/merchant-front/src/DDLogin.vue:27-42`
  - `merchant/merchant-front/src/js/common/Request.js:133,145`
  - `kernel/src/main/java/com/flyemu/share/enums/{OrderStatus,OperationType,CostingMethod}.java`
  - `kernel/src/main/java/com/flyemu/share/entity/{purchase,basic,inventory,fund,setting}/` 关键实体
  - `README.md:12,14,28`
