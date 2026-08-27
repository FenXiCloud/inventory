-- 更新所有单据制单人为蔡恩泽
-- 使用方法：
-- 1. 先查询蔡恩泽的用户ID：SELECT id FROM jxc_admin WHERE name = '蔡恩泽';
-- 2. 将下面的 @USER_ID 替换为实际ID
-- 3. 执行：mysql -u root -p jxc < update_created_by.sql

SET @USER_ID = 1;  -- 请替换为蔡恩泽的实际用户ID

-- 采购相关
UPDATE jxc_purchase_order SET created_by = @USER_ID;
UPDATE jxc_purchase_order_item SET created_by = @USER_ID;
UPDATE jxc_purchase_inbound SET created_by = @USER_ID;
UPDATE jxc_purchase_inbound_item SET created_by = @USER_ID;
UPDATE jxc_purchase_return SET created_by = @USER_ID;
UPDATE jxc_purchase_return_item SET created_by = @USER_ID;
UPDATE jxc_purchase_reservation SET created_by = @USER_ID;
UPDATE jxc_purchase_reservation_item SET created_by = @USER_ID;

-- 销售相关
UPDATE jxc_sales_order SET created_by = @USER_ID;
UPDATE jxc_sales_order_item SET created_by = @USER_ID;
UPDATE jxc_sales_outbound SET created_by = @USER_ID;
UPDATE jxc_sales_outbound_item SET created_by = @USER_ID;
UPDATE jxc_sales_return SET created_by = @USER_ID;
UPDATE jxc_sales_return_item SET created_by = @USER_ID;
UPDATE jxc_sales_reservation SET created_by = @USER_ID;
UPDATE jxc_sales_reservation_item SET created_by = @USER_ID;

-- 库存相关
UPDATE jxc_inventory_transfer SET created_by = @USER_ID;
UPDATE jxc_inventory_transfer_item SET created_by = @USER_ID;
UPDATE jxc_stock_take SET created_by = @USER_ID;
UPDATE jxc_other_inbound SET created_by = @USER_ID;
UPDATE jxc_other_inbound_item SET created_by = @USER_ID;
UPDATE jxc_other_outbound SET created_by = @USER_ID;
UPDATE jxc_other_outbound_item SET created_by = @USER_ID;
UPDATE jxc_cost_adjustment SET created_by = @USER_ID;
UPDATE jxc_cost_adjustment_item SET created_by = @USER_ID;
UPDATE jxc_assembly_order SET created_by = @USER_ID;
UPDATE jxc_location_transfer SET created_by = @USER_ID;
UPDATE jxc_pick_order SET created_by = @USER_ID;
UPDATE jxc_inventory_item SET created_by = @USER_ID;

-- 资金相关
UPDATE jxc_order_receipt SET created_by = @USER_ID;
UPDATE jxc_order_payment SET created_by = @USER_ID;
UPDATE jxc_verification SET created_by = @USER_ID;
UPDATE jxc_settlement SET created_by = @USER_ID;
UPDATE jxc_account_transfer SET created_by = @USER_ID;
UPDATE jxc_other_receipt SET created_by = @USER_ID;
UPDATE jxc_other_expense SET created_by = @USER_ID;
UPDATE jxc_customer_flow SET created_by = @USER_ID;
UPDATE jxc_supplier_flow SET created_by = @USER_ID;
UPDATE jxc_account_flow SET created_by = @USER_ID;

-- 系统相关
UPDATE jxc_system_log SET created_by = @USER_ID;
UPDATE jxc_data_backup SET created_by = @USER_ID, created_by_name = '蔡恩泽';
UPDATE jxc_finance_voucher SET created_by = @USER_ID;
