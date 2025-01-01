# 项目介绍
纷析云SAAS多租户进销存系统是纷析云（杭州）科技有限公司旗下开源产品吗，本源码完全开放，可二开，可商用，遵循Apache2.0协议。


#### 系统演示
系统重构中。。。。

- https://jxc.fenxi365.com  商户后台
- 账号：13944878765
- 密码：878765
- https://jadmin.fenxi365.com  管理后台
- 账号：13944878765
- 密码：878765

#### 技术架构

- spring boot 3 + vue

#### 开发环境

- IDE: IntelliJ IDEA
- DB: Mysql 8.0
- JDK: JDK 17
- Node: Node 16+
- Gradle: Gradle
- Redis: 6.0+

#### 本地部署
1.初始化根目录数据库脚本jxc.sql
2.修改项目jdk版本17
3.修改Gradle jdk版本17
4.前端运行
cd admin-front  yarn install
cd merchant-front  yarn install
5.配置数据库kernel/src/resources/application.yml
6.启动项目 
admin初始账号密码：13944878765 878765
merchant 初始账号密码：13944878765 878765

#### 数据字典
- 设置
  - system_config               系统参数配置 
  - pricing_policy              价格策略（销售采购价格取数优先级）
  - code_rule                   编码规则
  - account_book                账套
  - checkout                    结账
  - admin                       管理员
  - role                        角色
  - menu                        菜单
  - menu_role                   角色菜单
  - merchant                    商户
  - merchant_menu               商户菜单
  - merchant_user               商户用户
- 资料
  - customer_category           客户分类
  - customer_level              客户等级
  - customer_level_price        客户等级价格
  - customer                    客户档案
  - product_category            产品分类
  - product                     产品档案
  - supplier_category           供货商分类
  - supplier                    供货商档案
  - unit                        计量单位
  - warehouse                   仓库档案
  - account                     账户管理
  - account_flow                账户流水
  - account_type                收支类型
  - payment_method              结算方式
- 资金
  - receipt                    收款单
  - receipt_item               收款单明细
  - payment                    付款单
  - payment_item               付款单明细
  - verification               核销单
  - verification_item          核销单明细
  - account_transfer           账户转账
  - account_transfer_item      账户转账明细
  - other_receipt              其他收款单
  - other_receipt_item         其他收款单明细
  - other_payment              其他付款单
  - other_payment_item         其他付款单明细
- 采购：
  - purchase_order              采购订单
  - purchase_order_item         采购订单明细
  - purchase_inbound            采购入库单
  - purchase_inbound_item       采购入库单明细
  - purchase_return             采购退货单
  - purchase_return_item        采购退货单明细
  - purchase_price_record       采购价格记录
- 销售：
  - sales_order                 销售订单
  - sales_order_item            销售订单明细
  - sales_outbound              销售出库单
  - sales_outbound_item         销售出库单明细
  - sales_return                销售退货单
  - sales_return_item           销售退货单明细
  - sales_price_record          销售价格记录
- 库存：
  - other_outbound              其他出库单
  - other_outbound_item         其他出库单明细
  - other_inbound               其他入库单
  - other_inbound_item          其他入库单明细
  - inventory                   库存表
  - inventory_item              库存明细表
  - inventory_transfer          库存调拨单
  - inventory_transfer_item     库存调拨单明细
  - stock_take                  库存盘点
  - stock_take_item             库存盘点明细
  - cost_adjustment             成本调整单
  - cost_adjustment_item        成本调整单明细


#### 如何支持
- 开源不易，坚持更难！如果您觉得纷析云进销存不错，不用请作者喝咖啡。
- 如果您有任何问题可以添加客服进群
<img src="https://f3.fenxi365.com/api/assets/logo/4d6614c2-4384-487f-ba73-d8a4439f2033.png" height="150px" width="150px"/>

