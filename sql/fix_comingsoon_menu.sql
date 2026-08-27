-- 修复 17 个「开发中」占位菜单的 component 指向
-- 本地 jxc 库执行即可（admin-server 不启动不 seed 菜单）
USE jxc;

UPDATE jxc_menu SET component = 'ProductAttributeList'   WHERE id = 239; -- 辅助属性
UPDATE jxc_menu SET component = 'CreditLimit'            WHERE id = 240; -- 信用额度(往来单位)
UPDATE jxc_menu SET component = 'PurchaseInboundImport'  WHERE id = 206; -- 进货单导入
UPDATE jxc_menu SET component = 'PurchaseStatisticsReport' WHERE id = 210; -- 采购统计表
UPDATE jxc_menu SET component = 'SalesDrivenPurchase'    WHERE id = 208; -- 以销定购看板(采购增强)
UPDATE jxc_menu SET component = 'SmartReplenishment'     WHERE id = 209; -- 智能补货
UPDATE jxc_menu SET component = 'SalesDrivenPurchase'    WHERE id = 202; -- 以销定购看板(销售增强)
UPDATE jxc_menu SET component = 'ScanOrder'              WHERE id = 203; -- 扫码开单
UPDATE jxc_menu SET component = 'SalesStatisticsReport'  WHERE id = 204; -- 销售统计表
UPDATE jxc_menu SET component = 'OrderTracking'          WHERE id = 205; -- 订单跟踪
UPDATE jxc_menu SET component = 'AssemblyOrderList'      WHERE id = 211; -- 组装拆卸单
UPDATE jxc_menu SET component = 'BatchTracking'          WHERE id = 213; -- 批次管理
UPDATE jxc_menu SET component = 'CreditLimit'            WHERE id = 222; -- 信用额度(资金增强)
UPDATE jxc_menu SET component = 'BatchInvoice'           WHERE id = 227; -- 批量开票
UPDATE jxc_menu SET component = 'InvoiceStatistics'      WHERE id = 231; -- 开票统计
UPDATE jxc_menu SET component = 'InvoiceQuota'           WHERE id = 232; -- 剩余额度
UPDATE jxc_menu SET component = 'TaxConfig'              WHERE id = 245; -- 开票默认项（复用已有销方默认设置页）
