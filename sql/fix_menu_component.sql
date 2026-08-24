-- ============================================================================
-- 修复报表菜单缺少 component（前端组件名），导致无法点击打开
-- 对应警告: [AppMenu] 菜单缺少 component/key，无法打开
--
-- 执行前请确认这些菜单的 ID 和 name 与数据库一致
-- 执行后需刷新页面或重新登录才能看到效果
-- ============================================================================

-- 先查看当前记录，确认 ID 和名称是否正确
SELECT id, name, component, parent_id, menu_group
FROM jxc_menu
WHERE id IN (37, 38, 39, 40, 41)
ORDER BY id;

-- 更新 component 字段（表名 jxc_menu 由 SharePhysicalNamingStrategy 的 jxc_ 前缀决定）
UPDATE jxc_menu SET component = 'CounterpartDebt'    WHERE id = 37 AND name = '往来单位欠款表';
UPDATE jxc_menu SET component = 'CustomerStatements' WHERE id = 38 AND name = '客户对账单';
UPDATE jxc_menu SET component = 'VendorStatements'   WHERE id = 39 AND name = '供货商对账单';
UPDATE jxc_menu SET component = 'CustomerFlowReport' WHERE id = 40 AND name = '应收账款明细表';
UPDATE jxc_menu SET component = 'SupplierFlowReport' WHERE id = 41 AND name = '应付账款明细表';

-- 确认更新结果
SELECT id, name, component,
       CASE WHEN component IS NOT NULL AND component != '' THEN 'OK' ELSE 'MISSING' END AS status
FROM jxc_menu
WHERE id IN (37, 38, 39, 40, 41)
ORDER BY id;
