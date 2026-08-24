-- 菜单结构调整：预订移入单据分组 + 去掉增强分组 + 合并智能补货 + 按使用频率排序
-- 生产环境手动执行（不启动 admin-server 时）
-- 执行方式：mysql -u root -p jxc < menu_restructure.sql
USE jxc;

-- 1. 销售预订移入「销售单据」
UPDATE jxc_menu SET parent_id = 58, pos = 3 WHERE id = 257;

-- 2. 进货预订移入「采购单据」
UPDATE jxc_menu SET parent_id = 51, pos = 3 WHERE id = 258;

-- 3. 扫码开单移入「销售单据」
UPDATE jxc_menu SET parent_id = 58, pos = 4 WHERE id = 203;

-- 4. 智能补货移入「采购单据」
UPDATE jxc_menu SET parent_id = 51, pos = 5 WHERE id = 209;

-- 5. 禁用废弃菜单（含以销定购看板，已合并到智能补货）
UPDATE jxc_menu SET enabled = 0 WHERE id IN (200, 201, 202, 207, 208, 259, 245);

-- 6. 销售单据内排序
UPDATE jxc_menu SET pos = 0 WHERE id = 61;  -- 销售出库单
UPDATE jxc_menu SET pos = 1 WHERE id = 60;  -- 销售订单
UPDATE jxc_menu SET pos = 2 WHERE id = 62;  -- 销售退货单
UPDATE jxc_menu SET pos = 3 WHERE id = 257; -- 销售预订
UPDATE jxc_menu SET pos = 4 WHERE id = 203; -- 扫码开单

-- 7. 采购单据内排序
UPDATE jxc_menu SET pos = 0 WHERE id = 53;  -- 采购入库单
UPDATE jxc_menu SET pos = 1 WHERE id = 52;  -- 采购订单
UPDATE jxc_menu SET pos = 2 WHERE id = 54;  -- 采购退货单
UPDATE jxc_menu SET pos = 3 WHERE id = 258; -- 进货预订
UPDATE jxc_menu SET pos = 4 WHERE id = 206; -- 进货单导入
UPDATE jxc_menu SET pos = 5 WHERE id = 209; -- 智能补货

-- 8. 销售/采购管理子组 pos 调整
UPDATE jxc_menu SET pos = 1 WHERE id = 59;  -- 销售报表
UPDATE jxc_menu SET pos = 1 WHERE id = 55;  -- 采购报表
