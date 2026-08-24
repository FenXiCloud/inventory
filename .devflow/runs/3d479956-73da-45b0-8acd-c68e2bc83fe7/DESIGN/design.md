# 进销存ERP系统设计说明书

> 项目：纷析云进销存（Demo）
> 阶段：DESIGN
> 文档状态：**待干系人评审确认（DRAFT）**
> 运行目录：`.devflow/runs/3d479956-73da-45b0-8acd-c68e2bc83fe7/DESIGN/`

---

## 0. 文档信息

| 项目 | 内容 |
|------|------|
| 文档名称 | 进销存ERP系统设计说明书 |
| 项目名称 | Demo（纷析云进销存 ERP） |
| 编写阶段 | DevFlow DESIGN |
| 编写日期 | 2026-08-24 |
| 依据来源 | ANALYZE 产物 `prd.md`（需求规格说明书 DRAFT）、仓库代码现状（`kernel` / `merchant` / `admin` / `app`）；`docs/**` 目录在工作区内不存在，按任务约定跳过 |
| 评审状态 | 待干系人评审确认（见第 10 章） |

### 0.1 与前序产物的关系

- 本设计说明书以 ANALYZE 阶段 `prd.md` 确认的需求为输入，将需求翻译为可实施的系统设计（架构、流程、模块、数据、接口、权限）；
- 需求条目编号（FR-*/NFR-*/ISS-*）与 `prd.md` 保持一致，见第 11.3 章追溯矩阵；
- 本文档通过干系人评审确认后，作为 DEVELOP 阶段的实施依据。

---

## 1. 设计概述

### 1.1 设计目标

在现有代码基础上交付一套可落地、可验证、可演进的设计方案，支撑 prd.md 的六大目标（G1 业务一体化、G2 库存精细化、G3 资金闭环、G4 多租户 SaaS 化、G5 开箱即用、G6 可扩展）。

### 1.2 设计原则

1. **现状优先（Surgical）**：以仓库现有架构与代码事实为准，不引入与现状冲突的新框架；设计变更最小化。
2. **需求闭环**：每个功能需求在设计层有明确落点（模块/接口/数据），每个已知问题有处理设计。
3. **事务一致**：单据审核、库存变动、资金变动同事务；成本核算可追溯。
4. **租户隔离**：所有业务数据按商户（Merchant）+ 账套（AccountBook）维度隔离，贯穿查询与写入。
5. **评审确认**：设计通过干系人评审确认后方可进入开发（见第 10 章）。

### 1.3 设计范围

覆盖 prd.md 第 5 章全部功能需求（P0/P1/P2）的设计落地；其中 P2（移动端完整业务）仅给出演进方向，不展开详细设计。本期不含生产制造、多级审批流、全量财务总账（见 prd.md 第 7 章非目标）。

---

## 2. 总体架构设计

### 2.1 系统组成与技术栈（现状事实）

| 组成 | 技术栈 | 职责 |
|------|--------|------|
| `kernel` 核心模块 | Java 17 + Spring Boot + JPA + Sa-Token | 共享实体、枚举、仓库、服务、异常、Web 配置（`WebMvcSupport` 统一鉴权） |
| `merchant/merchant-server` | Spring Boot，端口 8410（application-app.yml） | 商户端业务 API：基础资料/采购/销售/库存/资金/设置/登录认证 |
| `merchant/merchant-front` | Vue 3 + TDesign + axios + vuex | 商户端页面，`/api` 前缀代理 |
| `admin/admin-server` | Spring Boot + Sa-Token | 平台管理 API：商户/菜单/平台用户；登录 login 主键为 `"admin"+userId` |
| `admin/admin-front` | Vue 3 + TDesign | 平台管理端页面 |
| `app` | uni-app（.uvue） | 移动端：目前仅登录页 + 首页框架 |

### 2.2 模块依赖

```
admin-front / merchant-front / app（前端）
        │ HTTP JSON（JsonResult 统一包装）
        ▼
merchant-server ──► kernel（实体/枚举/仓库/服务基类）
admin-server   ──► kernel
        │
        ▼
数据库（业务数据按 merchantId + accountBookId 隔离）
Redis（缓存 / Sa-Token 会话）
```

### 2.3 关键技术决策

| 决策点 | 结论 | 依据 |
|--------|------|------|
| 认证方案 | Sa-Token（token-name `i-c-m-token`），登录后 `StpUtil.login`，`/init` 装载菜单与账套 | merchant-server `AppController`、application-app.yml |
| 统一返回 | `JsonResult`（success/msg/data），前端按 `data.success` 分支 | `JsonResult`、`Request.js` |
| 前端路由鉴权 | 依赖 `/init` 返回 account 判断登录态，403 统一 `window.location.replace("/")` | `App.vue`、`Request.js` |
| 成本核算 | 账套参数 `costAccounting`（1=移动加权平均，2=先进先出），`CostingService` + `InventoryCostBatch`/`InventoryCostConsume` | `CostingMethod` 枚举、inventory 服务 |
| 租户隔离 | 商户/账套维度过滤，注解 `@SaAccountVal` 注入当前登录账户 | `annotation`、`resolver` 目录 |

---

## 3. 关键流程设计

### 3.1 登录认证流程（含 ISS-01 Login broken 处理设计）

现状链路（代码事实）：

```
merchant-front main.js
  └─ store.dispatch('init')  →  GET /init（携带 Sa-Token Cookie）
       ├─ 成功：写入 user/menus/accountBooks → App.vue 按 user.merchantId 渲染 AppFrame
       └─ 失败：mount 后 App.vue 显示 Login / DDLogin

Login.vue submitForm
  └─ POST /api/login (username, password, device)
       └─ merchant-server AppController.login
            ├─ adminService.login(username, password) 校验
            ├─ StpUtil.login(adminId, device|"pc")，session 存 Constants.SESSION_ACCOUNT
            ├─ response.addHeader("Authorization", token)  ← 通过响应头下发 token
            └─ 返回 account；前端成功后 window.location.replace('/')
```

设计目标：登录链路稳定可用，登录态失效自动回登录页且无死循环。

**SS-01 登录链路设计（对应 ISS-01）**

| 设计项 | 内容 |
|--------|------|
| 根因分析 | 按「前端请求 → 网关/代理 → 后端接口 → Sa-Token 会话 → 前端登录态判定」逐层排查。代码勘察发现的候选风险点：① `/init` 未登录时返回 `JsonResult.failure()`，前端 catch 后仍 mount，依赖 `App.vue` 的 `user.merchantId` 分支显示登录页，若 account 结构变化会导致页面空白；② token 经响应头 `Authorization` 下发，前端 `Request.js` 未显式存储/携带该头，依赖 Sa-Token Cookie（`i-c-m-token`），若 Cookie 域/路径/跨域配置与部署环境不符，后续请求 403 → `window.location.replace("/")` 可能形成重定向循环；③ `DDLogin.vue` 读取 `corpId` 时 localStorage 键为 `cropId`（疑似笔误），钉钉环境可能取不到 corpId；④ `Login.vue created()` 自动调用 `loginByMobile()`，无 `mobile` 参数时直接 return，不影响主流程 |
| 诊断步骤 | ① 复现并抓取浏览器 Network（/login 响应头是否含 `i-c-m-token` Cookie、后续 /init 是否携带）；② 核对部署环境域名/Cookie 域与 Sa-Token 配置；③ 单独调用 /init 与 /login 验证后端行为；④ 检查前端构建产物与代理配置（`/api` 前缀） |
| 修复设计 | ① 前端统一封装登录态判定：`/init` 失败即明确展示登录页，避免空白页与重定向循环；② `Request.js` 显式保存登录响应 `Authorization`/Cookie 并在后续请求携带（与后端 token-name 一致）；③ 修正 `DDLogin.vue` localStorage 键名笔误（`cropId` → `corpId`）；④ 后端 `/login` 增加失败信息明确化（账号不存在/密码错误/账号停用区分提示） |
| 回归验证 | ① 账号密码正确 → 登录成功进入首页；② 密码错误 → 明确错误提示；③ 未登录直接访问业务页 → 跳转登录页且无死循环；④ 登录态过期 → 自动回登录页；⑤ 钉钉环境走 DDLogin 流程可获取 corpId |

### 3.2 采购流程设计（对应 prd 4.1）

```
采购订单(PurchaseOrder) 保存 → 审核(OrderStatus: 已保存→未审核→已审核)
  └─审核后──► 采购入库(PurchaseInbound, orderId) ──审核──► 库存+(InventoryItem/OperationType=采购入库) + 应付+(SupplierFlow)
       └─ 采购退货(PurchaseReturn, 经 PurchaseInboundReturnConnection) ──审核──► 库存- + 应付冲减
```

设计要点：
- 单据状态统一走 `OrderStatus` 枚举，审核操作在服务层同事务完成「状态流转 + 库存流水 + 往来流水」；
- 部分入库/超收控制：按 `orderId` 关联的入库数量与订单数量校验；
- 编码规则：单据编号由 `CodeRule` + `CodeSeed` 生成（按天/月/年/不重置）。

### 3.3 销售流程设计（对应 prd 4.2）

```
销售订单(SalesOrder) 保存 → 审核 → 销售出库(SalesOutbound, orderId) ──审核──► 库存- + 应收+(CustomerFlow)
       └─ 销售退货(SalesReturn) ──审核──► 库存回补 + 应收冲减
       └─ 以销订购：依据销售订单联动生成补货/采购建议
```

### 3.4 库存与成本核算流程设计（对应 prd 4.3）

- 所有库存变动统一记 `InventoryItem` 流水（`OperationType` 区分业务来源），入库/出库成对出现的业务（调拨、盘点差异）在同一事务内提交；
- 成本核算：`CostingService` 按账套参数选择移动加权平均或先进先出，成本批次 `InventoryCostBatch`/`InventoryCostConsume` 记录消耗链，支持追溯；
- 负库存控制：`Merchant.stockField` 开关，未开启时出库前校验库存充足性，禁止并发产生负库存（行级锁/乐观锁）。

### 3.5 资金流程设计（对应 prd 4.4）

```
收款单(OrderReceipt) → 核销单(Verification, VerificationCollection) → 冲销应收(CustomerFlow) + 账户增加(AccountFlow)
付款单(OrderPayment) → 核销单 → 冲销应付(SupplierFlow) + 账户减少(AccountFlow)
其他收支(OtherReceipt/OtherExpense) / 账户转账(AccountTransfer) 独立记账
```

设计要点：
- 支持整单核销与部分核销，未核销余额通过 `SourceByVerification` 视图追踪；
- 收支类别可映射财务科目（`FinanceItemMap`），为 `FinanceVoucher` 与 `Checkout` 提供基础。

---

## 4. 模块详细设计

### 4.1 登录与账户（P0）

| 需求 | 设计落点 |
|------|----------|
| FR-AUTH-01 登录认证 | merchant `AppController.login`；admin `AppController.login`（`"admin"+userId`）；`AdminService.login` 密码校验（非明文存储） |
| FR-AUTH-02 租户数据隔离 | `@SaAccountVal` 注入 AccountDto，服务层按 merchantId/accountBookId 过滤；`@SaAdminId` 注入员工 ID |
| FR-AUTH-03 菜单级权限 | `AdminService.loadMenu(merchantId, role)`；前端 `v-auth` 指令 + `granted` 菜单列表 |
| FR-AUTH-04 钉钉登录 | `DDLogin.vue` → `/dd/auth` → `DDLoginServiceImpl`（钉钉 jsapi + 服务端换取用户信息） |
| FR-AUTH-05 密码修改/停启用/留痕 | `SystemLog` 记录登录/退出/关键操作；员工停用/启用由商户管理员操作 |

### 4.2 基础资料（P0/P1，对应 FR-BAS-*）

- 商品（Product + ProductCategory + Unit + ProductPrice）、客户/供应商（Customer/Supplier + 分类/等级 + 期初应收应付 + CustomerImport）、仓库（Warehouse）、资金账户（Account + AccountType）、价格策略（PricingPolicy/PriceRecord）；
- 期初库存（InventoryInitial）：按仓库/批次录入，审核后生成 `OperationType=期初库存` 流水；
- 智能补货（P1）：基于库存与销售预测生成补货建议（现状提交记录已含）。

### 4.3 采购模块（P0，对应 FR-PUR-*）

| 需求 | 设计落点 |
|------|----------|
| FR-PUR-01 采购订单 | PurchaseOrder 服务：录入/保存/审核/详情，编码规则生成单号 |
| FR-PUR-02 采购入库 | PurchaseInbound 服务：由订单生成（orderId），部分入库与超收控制 |
| FR-PUR-03 采购退货 | PurchaseReturn 服务：经 PurchaseInboundReturnConnection 关联入库单，审核冲减库存与应付 |
| FR-PUR-04 采购报表（P1） | PurchaseSummaryReport / PurchaseItemReport |

### 4.4 销售模块（P0，对应 FR-SAL-*）

| 需求 | 设计落点 |
|------|----------|
| FR-SAL-01 销售订单 | SalesOrder 服务：录入/保存/审核/详情；以销订购联动 |
| FR-SAL-02 销售出库 | SalesOutbound 服务：由订单生成，部分出库控制 |
| FR-SAL-03 销售退货 | SalesReturn 服务：基于出库单，审核回补库存、冲减应收 |
| FR-SAL-04 销售报表（P1） | SalesProfitReport / SalesRankingReport 等 |
| FR-SAL-05 电子发票（P1） | 开票能力（现状提交记录已含，本期保持） |

### 4.5 库存模块（P0，对应 FR-INV-*）

| 需求 | 设计落点 |
|------|----------|
| FR-INV-01 库存查询 | InventoryReport / InventorySummaryReport / InventoryItemReport / InventoryCostBatchList |
| FR-INV-02 其他出入库 | OtherInbound/OtherOutbound（InboundType/OutboundType 含盘盈/盘亏） |
| FR-INV-03 调拨 | InventoryTransfer + InventoryTransferItem（出入库成对） |
| FR-INV-04 盘点 | StockTake + StockTakeWarehouse，差异自动生成盘盈/盘亏单 |
| FR-INV-05 成本调整（P1） | CostAdjustment（AdjustmentType 入库/出库调整），联动成本批次 |
| FR-INV-06 成本核算 | CostingService（移动加权平均/先进先出）+ InventoryCostBatch/Consume 追溯 |

### 4.6 资金模块（P0，对应 FR-FND-*）

| 需求 | 设计落点 |
|------|----------|
| FR-FND-01 收款/付款单 | OrderReceipt / OrderPayment + OrderReceiptCollection / OrderPaymentCollection |
| FR-FND-02 核销单 | Verification + VerificationCollection，未核销余额 SourceByVerification 追踪 |
| FR-FND-03 其他收支（P1） | OtherReceipt / OtherExpense 及类别 |
| FR-FND-04 账户转账（P1） | AccountTransfer |
| FR-FND-05 往来对账（P1） | CustomerFlow / SupplierFlow / AccountFlow + 收支类别报表 |

### 4.7 系统设置与平台（P0/P1，对应 FR-SET-*）

| 需求 | 设计落点 |
|------|----------|
| FR-SET-01 账套管理 | AccountBookService（多账套、成本核算参数、结账日期） |
| FR-SET-02 员工与角色 | Admin / Role / MenuRole / GrantMenu |
| FR-SET-03 编码规则（P1） | CodeRule + CodeSeed + ResetCycle |
| FR-SET-04 打印模板（P1） | PrintTemplate + PrintPreview |
| FR-SET-05 系统配置与日志（P1） | SystemConfig + SystemLog |
| FR-SET-06 备份恢复（P1） | DataBackup |
| FR-SET-07 结账（P1） | Checkout + FinanceRel / FinanceVoucher + FinanceItemMap |
| FR-SET-08 平台商户管理 | admin 端 Merchant / MerchantModuleGrant / Menu |

### 4.8 移动端（P2，对应 FR-APP-*）

- 现状：`app` 仅 `pages/login/login` 与 `pages/index/index` 两页（uni-app .uvue）；
- 演进方向：复用 merchant-server 既有 API，按需覆盖查询、审批、开单等高频场景；本期不展开详细设计。

---

## 5. 数据模型设计

### 5.1 核心实体关系（与 prd 2.2 对应，字段以代码为准）

```
Merchant 1─n AccountBook 1─n Admin（员工）n─n Role（角色，经 RoleAdmin/GrantMenu 授权）
Merchant 1─n Warehouse / Customer / Supplier / Product(ProductCategory, Unit, ProductPrice) / Account
PurchaseOrder 1─n PurchaseInbound 1─n PurchaseReturn（经 PurchaseInboundReturnConnection）
SalesOrder 1─n SalesOutbound 1─n SalesReturn
单据/流水：InventoryItem（OperationType）← Inventory / InventoryCostBatch / InventoryCostConsume
资金：OrderReceipt / OrderPayment / Verification / AccountFlow / CustomerFlow / SupplierFlow
设置：CodeRule+CodeSeed / PrintTemplate / SystemConfig / SystemLog / DataBackup / Checkout / FinanceVoucher
```

### 5.2 关键约束设计

| 约束 | 设计 |
|------|------|
| 单据编号 | `CodeRule` 规则 + `CodeSeed` 序列，唯一索引，并发取号事务安全 |
| 库存一致性 | 审核动作在单事务内：状态流转 + 库存流水 + 往来流水；禁止跨事务先改后补 |
| 成本追溯 | `InventoryCostConsume` 记录批次消耗来源，支持按批次/单据反查 |
| 租户隔离 | 业务表均含 merchantId（必要处 accountBookId）过滤条件，查询层强制注入 |
| 数据备份 | `DataBackup` 支持手动/定期备份与恢复 |

---

## 6. 接口设计

### 6.1 统一约定

- 前缀：前端请求经 `/api` 代理；返回统一 `JsonResult{success, msg, data}`；
- 鉴权：Sa-Token（token-name `i-c-m-token`），关键接口 `@SaCheckLogin`；
- 错误处理：`ErrorControllerAdvice` 统一异常 → `JsonResult`（500 前端提示「后台异常」，404「请求不存在」，403 跳登录）。

### 6.2 登录相关接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/login` | POST | 商户端登录（username/password/device），成功下发 token，返回 account |
| `/init` | GET | 初始化：返回 account + menus + accountBooks（未登录返回 failure） |
| `/logout` | GET | 退出登录，写 SystemLog |
| `/dd/auth` | POST | 钉钉 code 换登录态 |
| `/financial/login` | POST | 手机号快捷登录（前端 loginByBumer） |

### 6.3 业务接口（代表）

- 基础资料：`/basic/product`、`/basic/customer`、`/basic/supplier`、`/basic/warehouse` 等 CRUD + 导入；
- 采购：`/purchase/order`、`/purchase/inbound`、`/purchase/return`（保存/审核/详情/分页列表）；
- 销售：`/sales/order`、`/sales/outbound`、`/sales/return`；
- 库存：`/inventory/query`、`/inventory/item`、`/inventory/transfer`、`/inventory/stocktake`、`/inventory/cost/adjust`、`/inventory/cost/batch`；
- 资金：`/fund/receipt`、`/fund/payment`、`/fund/verification`、`/fund/transfer`、`/fund/flow`；
- 设置：`/setting/account-book`、`/setting/admin`、`/setting/role`、`/setting/code-rule`、`/setting/print-template`、`/setting/backup`。

---

## 7. 权限设计

- 模型：角色（Role）— 菜单（MenuRole / GrantMenu）— 员工（Admin）三级授权；系统预置角色带 `systemDefault` 标记；
- 平台端：admin 管理 `Merchant`、`MerchantModuleGrant`、菜单；
- 商户端：员工登录后 `/init` 返回已授权菜单树与 `granted` 权限码，前端 `v-auth` 指令控制按钮级显示；
- 数据权限：按商户/账套隔离（见 5.2）；
- 预置角色建议（与 prd 3.2 一致）：商户管理员、采购岗、销售岗、仓管岗、财务/出纳岗、基础资料维护岗。

---

## 8. 已知问题处理设计

| 编号 | 问题 | 状态 | 处理设计（详见本章） |
|------|------|------|----------------------|
| ISS-01 | Login broken | [OPEN] | 3.1 节 SS-01：根因分析 → 修复设计（4 项）→ 回归验证（5 用例）；排入最近迭代 P0 |
| ISS-02 | 移动端仅登录页与首页框架 | [OPEN] | 列为 P2 规划项（4.8 节演进方向），不阻塞本期交付 |
| ISS-03 | 平台端/商户端 docs 文档缺失 | [OPEN] | 评审时确认是否补充项目规范文档；本期交付物（prd.md/design.md）先行沉淀 |

问题管理流程（与 prd 8.2 一致）：评审逐条确认 → 阻断级优先排期 → 修复附回归记录 → 关闭需评审确认（[OPEN] → [FIXED]/[CLOSED]/[WONTFIX]）。

---

## 9. 非功能设计（对应 prd 第 6 章）

| 编号 | 类别 | 设计措施 |
|------|------|----------|
| NFR-01 | 性能 | 常规单据（<100 行）操作 ≤ 2s；库存流水查询分页 + 索引（merchantId/accountBookId/operationType/date），千万级响应 ≤ 3s；Redis 缓存字典/菜单 |
| NFR-02 | 数据一致性 | 审核事务化（见 5.2）；并发出库防负库存（开关未开时校验 + 行锁）；成本批次可追溯 |
| NFR-03 | 安全 | 密码非明文（DigestUtil 摘要）；登录限流/防爆破；Sa-Token 会话；租户隔离；关键操作 SystemLog |
| NFR-04 | 可用性 | 关键单据列表/详情/打印；表单即时校验（TDesign 规则） |
| NFR-05 | 兼容性 | Chrome/Edge/Firefox；uni-app 多端 |
| NFR-06 | 可维护性 | 遵循 Java 文档注释规范与前端既有风格；新增功能同步更新 prd/design |
| NFR-07 | 可扩展性 | 菜单级模块授权可扩展；预留财务凭证/发票扩展点 |
| NFR-08 | 数据备份 | DataBackup 手动/定期备份与恢复 |
| NFR-09 | 文档与交付 | 本文档与 prd.md 通过干系人评审确认后进入开发 |

---

## 10. 干系人评审与确认

### 10.1 评审要求

- 本设计说明书须经项目干系人（产品/技术/测试代表）评审通过并**确认签字**后方可进入 DEVELOP 阶段；
- 评审范围：设计完整性（第 2–7 章）、与需求一致性（追溯矩阵 11.3）、已知问题处理方案（第 8 章）、非功能设计合理性（第 9 章）；
- 评审结论可为：通过 / 有条件通过（附整改项）/ 不通过（返工）。

### 10.2 确认记录

| 日期 | 评审人 | 结论 | 意见/备注 |
|------|--------|------|-----------|
| （待填写） | （待填写） | 待评审 | — |

> 本文档当前为 DRAFT，评审通过后更新为 APPROVED。评审整改项如有变更，需同步更新 prd.md 与本文档相关章节。

---

## 11. 附录

### 11.1 术语表（沿用 prd 10.1）

账套（AccountBook）、核销（Verification）、成本批次（InventoryCostBatch）、移动加权平均/先进先出、期初建账、以销订购、结账（Checkout）。

### 11.2 参考文件

- `prd.md`（`.devflow/runs/3d479956-73da-45b0-8acd-c68e2bc83fe7/ANALYZE/prd.md`）
- 仓库代码：`kernel/src/main/java/com/flyemu/share/{entity,enums,repository,service}`、`merchant/merchant-server/src/main/java/com/flyemu/share/{controller,service}`、`merchant/merchant-front/src/{Login.vue,DDLogin.vue,App.vue,js/store.js,js/common/Request.js}`、`admin/admin-server/src/main/java/com/flyemu/share/controller/AppController.java`
- 配置：`merchant/merchant-server/src/main/resources/application-app.yml`（端口 8410、sa-token.token-name=i-c-m-token、dingtalk 参数）
- 任务需求描述（DevFlow，本运行）

### 11.3 需求追溯矩阵（FR → 设计章节）

| 需求组 | 需求条目 | 设计章节 |
|--------|----------|----------|
| 登录/权限 | FR-AUTH-01 ~ 05 | 3.1、4.1、6.2、7 |
| 基础资料 | FR-BAS-01 ~ 06 | 4.2、5、6.3 |
| 采购 | FR-PUR-01 ~ 04 | 3.2、4.3、5、6.3 |
| 销售 | FR-SAL-01 ~ 05 | 3.3、4.4、5、6.3 |
| 库存 | FR-INV-01 ~ 06 | 3.4、4.5、5、6.3 |
| 资金 | FR-FND-01 ~ 05 | 3.5、4.6、5、6.3 |
| 设置/平台 | FR-SET-01 ~ 08 | 4.7、5、6.3、7 |
| 移动端 | FR-APP-01 ~ 02 | 4.8 |
| 非功能 | NFR-01 ~ 09 | 9 |
| 已知问题 | ISS-01 ~ 03 | 3.1（SS-01）、8 |

---

> 本文档为 DESIGN 阶段产物。评审通过并确认后，可作为 DEVELOP 阶段实施依据；如评审提出整改，修订后需复审确认。
