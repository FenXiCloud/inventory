package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.setting.*;
import com.flyemu.share.repository.setting.*;
import com.querydsl.core.BooleanBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MerchantService extends BaseService {

    private static final QMerchant qMerchant = QMerchant.merchant;

    private static final QMenu qMenu = QMenu.menu;

    private final MerchantRepository merchantRepository;

    private final AdminRepository adminRepository;

    private final RoleRepository roleRepository;

    private final AccountBookRepository accountBookRepository;

    private final MenuRepository menuRepository;

    private final MerchantMenuRepository merchantMenuRepository;

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initDefaultUser() {
        ensureDefaultMenus();
        // 为所有已有商户补齐新增菜单的关联
        List<Long> allMerchantIds = jqf.selectFrom(qMerchant).select(qMerchant.id).fetch();
        for (Long merchantId : allMerchantIds) {
            associateMissingMenus(merchantId);
        }
        Long count = jqf.selectFrom(qMerchant).select(qMerchant.count()).fetchFirst();
        if (count == 0) {
            Merchant merchant = new Merchant();
            merchant.setCode(UUID.randomUUID().toString());
            merchant.setName("纷析云");
            merchant.setAddress("杭州");
            merchant.setCreatedAt(LocalDateTime.now());
            merchant.setContact("李泽龙");
            merchant.setMobile("13944878765");
            merchant.setEnabled(true);
            this.save(merchant);

            // 初始化菜单数据
            initMenus(merchant.getId());

            log.info("测试商户账号：13944878765，密码：878765");
        } else {
            // 已有商户：补齐新增菜单关联
            List<Long> merchantIds = jqf.selectFrom(qMerchant).select(qMerchant.id).fetch();
            for (Long merchantId : merchantIds) {
                associateMissingMenus(merchantId);
            }
        }
    }

    public PageResults<Merchant> query(Page page, Query query) {
        PagedList<Merchant> fetchPage = bqf.selectFrom(qMerchant)
                .where(query.builder)
                .orderBy(qMerchant.enabled.desc(), qMerchant.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        return new PageResults<>(fetchPage, page);
    }

    @Transactional
    public Merchant save(Merchant merchant) {
        if (merchant.getId() != null) {
            //更新
            Merchant original = merchantRepository.getById(merchant.getId());
            BeanUtil.copyProperties(merchant, original, CopyOptions.create().ignoreNullValue());
            return merchantRepository.save(original);
        } else {
            Assert.notBlank(merchant.getName(), "商户名称不允许为空~");
            Assert.notBlank(merchant.getContact(), "联系人不允许为空~");
            Assert.notBlank(merchant.getMobile(), "联系人电话不允许为空~");
            String mobile = merchant.getMobile().trim();
            Assert.isTrue(mobile.length() >= 6, "联系人电话格式不正确~");
            merchant.setMobile(mobile);

            merchant.setEnabled(true);
            merchant.setCreatedAt(LocalDateTime.now());
            if (StrUtil.isBlank(merchant.getCode())) {
                merchant.setCode(UUID.randomUUID().toString());
            }
            merchantRepository.save(merchant);

            //创建默认角色和管理员账号（初始密码为手机号后6位）
            Role role = new Role();
            role.setName("商户管理员");
            role.setSystemDefault(true);
            role.setMerchantId(merchant.getId());
            roleRepository.save(role);

            Admin admin = new Admin();
            admin.setName(merchant.getContact());
            admin.setMobile(mobile);
            admin.setUsername(mobile);
            admin.setPassword(DigestUtil.bcrypt(mobile.substring(mobile.length() - 6)));
            admin.setEnabled(true);
            admin.setMerchantId(merchant.getId());
            admin.setSystemDefault(true);
            admin.setRoleId(role.getId());
            adminRepository.save(admin);

            // 创建默认账套
            AccountBook accountBook = new AccountBook();
            accountBook.setMerchantId(merchant.getId());
            accountBook.setCurrent(true);
            accountBook.setName(merchant.getName());
            accountBook.setEnabled(true);
            accountBookRepository.save(accountBook);

            // 为新商户关联菜单
            associateMerchantMenus(merchant.getId());
        }
        return merchantRepository.save(merchant);
    }

    @Transactional
    public void delete(Long merchantId) {
        merchantRepository.deleteById(merchantId);
    }

    public List<Merchant> select() {
        return bqf.selectFrom(qMerchant).orderBy(qMerchant.code.asc()).fetch();
    }

    /**
     * 初始化菜单数据（仅首次启动时全量写入；后续由 ensureDefaultMenus 补齐）
     */
    private void initMenus(Long merchantId) {
        ensureDefaultMenus();
        associateMerchantMenus(merchantId);
    }

    /**
     * 按默认菜单清单补齐缺失菜单，并更新已有菜单的 component/name 等字段
     */
    @Transactional
    public void ensureDefaultMenus() {
        List<Menu> defaults = buildDefaultMenus();
        int added = 0;
        int updated = 0;
        for (Menu menu : defaults) {
            Menu existing = jqf.selectFrom(qMenu)
                    .where(qMenu.id.eq(menu.getId()))
                    .fetchFirst();
            if (existing == null) {
                insertMenuNative(menu);
                added++;
            } else {
                boolean changed = false;
                if (!Objects.equals(existing.getComponent(), menu.getComponent())) {
                    existing.setComponent(menu.getComponent());
                    changed = true;
                }
                if (!Objects.equals(existing.getName(), menu.getName())) {
                    existing.setName(menu.getName());
                    changed = true;
                }
                if (!Objects.equals(existing.getParentId(), menu.getParentId())) {
                    existing.setParentId(menu.getParentId());
                    changed = true;
                }
                if (!Objects.equals(existing.getPos(), menu.getPos())) {
                    existing.setPos(menu.getPos());
                    changed = true;
                }
                if (!Objects.equals(existing.getEnabled(), menu.getEnabled())) {
                    existing.setEnabled(menu.getEnabled());
                    changed = true;
                }
                if (changed) {
                    menuRepository.save(existing);
                    updated++;
                }
            }
        }
        if (added > 0 || updated > 0) {
            log.info("菜单数据同步完成，新增 {} 条，更新 {} 条", added, updated);
        }
    }

    /**
     * 用原生 SQL 插入菜单，保留显式指定的 id。
     * <p>
     * Menu.id 使用 {@code GenerationType.IDENTITY}，JPA 的 save() 在插入新实体时会忽略显式主键、
     * 交由数据库自增，导致 buildDefaultMenus() 里指定的 id 与 parentId 对不上、父子关联断裂。
     * 因此新增菜单必须走原生 INSERT 显式写入 id 列。
     */
    private void insertMenuNative(Menu menu) {
        jdbcTemplate.update(
                "INSERT INTO jxc_menu (id, component, name, icon_cls, parent_id, pos, enabled, menu_module, menu_type, menu_group) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                menu.getId(),
                menu.getComponent(),
                menu.getName(),
                menu.getIconCls(),
                menu.getParentId(),
                menu.getPos(),
                Boolean.TRUE.equals(menu.getEnabled()) ? 1 : 0,
                menu.getMenuModule() != null ? menu.getMenuModule().name() : null,
                menu.getMenuType() != null ? menu.getMenuType().name() : null,
                menu.getMenuGroup() != null ? menu.getMenuGroup().name() : null
        );
    }

    /**
     * 为新商户关联所有菜单
     */
    @Transactional
    public void associateMerchantMenus(Long merchantId) {
        associateMissingMenus(merchantId);
    }

    /**
     * 仅为商户补齐尚未关联的菜单，避免重复插入
     */
    @Transactional
    public void associateMissingMenus(Long merchantId) {
        QMerchantMenu qMerchantMenu = QMerchantMenu.merchantMenu;
        List<Long> existing = jqf.selectFrom(qMerchantMenu)
                .select(qMerchantMenu.menuId)
                .where(qMerchantMenu.merchantId.eq(merchantId))
                .fetch();
        Set<Long> existingSet = new HashSet<>(existing);
        List<Long> menuIds = jqf.selectFrom(qMenu).select(qMenu.id).fetch();
        List<MerchantMenu> merchantMenus = menuIds.stream()
                .filter(menuId -> !existingSet.contains(menuId))
                .map(menuId -> {
                    MerchantMenu mm = new MerchantMenu();
                    mm.setMenuId(menuId);
                    mm.setMerchantId(merchantId);
                    return mm;
                })
                .collect(Collectors.toList());
        if (merchantMenus.isEmpty()) {
            return;
        }
        merchantMenuRepository.saveAll(merchantMenus);
        log.info("为商户 {} 补齐菜单关联 {} 条", merchantId, merchantMenus.size());
    }

    /**
     * 构建默认菜单数据
     *
     * 顶级菜单顺序（pos）：销售管理 → 采购管理 → 库存管理 → 资金管理 → 发票管理 → 基础资料 → 系统设置
     * 说明：
     *  - 原有菜单 ID 沿用 1..88（含资金模块已存在的「结算单」id=88，勿占用）。
     *  - 【新增】菜单 ID 一律从 200 起，避免与历史数据冲突。
     *  - 【新增】占位菜单统一指向 ComingSoon 组件，业务功能待后续实现。
     */
    private List<Menu> buildDefaultMenus() {
        List<Menu> menus = new ArrayList<>();

        // ===== 销售管理 (根菜单) =====
        menus.add(menu(26L, "Sales", "销售管理", "shop", null, 0));
        // 销售单据
        menus.add(menu(58L, null, "销售单据", null, 26L, 0));
        menus.add(menu(61L, "SalesOutboundList", "销售出库单", null, 58L, 0));
        menus.add(menu(60L, "SalesOrderList", "销售订单", null, 58L, 1));
        menus.add(menu(62L, "SalesReturnList", "销售退货单", null, 58L, 2));
        menus.add(disabledMenu(257L, "SalesReservationList", "销售预订", null, 58L, 3));
        menus.add(menu(203L, "ScanOrder", "扫码开单", null, 58L, 4));
        // 销售报表
        menus.add(menu(59L, null, "销售报表", null, 26L, 1));
        menus.add(menu(63L, "SalesItemReport", "销售明细表", null, 59L, 0));
        menus.add(menu(64L, "SalesSummaryReport", "销售汇总表", null, 59L, 1));
        menus.add(menu(204L, "SalesStatisticsReport", "销售统计表", null, 59L, 2));
        menus.add(menu(65L, "SalesProfitReport", "销售利润表", null, 59L, 3));
        menus.add(menu(66L, "SalesRankingReport", "销售排行表", null, 59L, 4));
        menus.add(menu(205L, "OrderTracking", "订单跟踪", null, 59L, 5));

        // ===== 采购管理 (根菜单) =====
        menus.add(menu(25L, "Purchase", "采购管理", "cart", null, 1));
        // 采购单据
        menus.add(menu(51L, null, "采购单据", null, 25L, 0));
        menus.add(menu(53L, "PurchaseInboundList", "采购入库单", null, 51L, 0));
        menus.add(menu(52L, "PurchaseOrderList", "采购订单", null, 51L, 1));
        menus.add(menu(54L, "PurchaseReturnList", "采购退货单", null, 51L, 2));
        menus.add(disabledMenu(258L, "PurchaseReservationList", "进货预订", null, 51L, 3));
        menus.add(menu(206L, "PurchaseInboundImport", "进货单导入", null, 51L, 4));
        menus.add(menu(209L, "SmartReplenishment", "智能补货", null, 51L, 5));
        // 采购报表
        menus.add(menu(55L, null, "采购报表", null, 25L, 1));
        menus.add(menu(56L, "PurchaseItemReport", "采购明细表", null, 55L, 0));
        menus.add(menu(57L, "PurchaseSummaryReport", "采购汇总表", null, 55L, 1));
        menus.add(menu(210L, "PurchaseStatisticsReport", "采购统计表", null, 55L, 2));

        // ===== 库存管理 (根菜单) =====
        menus.add(menu(27L, "Inventory", "库存管理", "layers", null, 2));
        // 库存单据（按使用频率排序）
        menus.add(menu(67L, null, "库存单据", null, 27L, 0));
        menus.add(menu(75L, "InventoryTransferList", "调拨单", null, 67L, 0));
        menus.add(menu(76L, "StockTakeList", "盘点单", null, 67L, 1));
        menus.add(menu(78L, "OtherInboundList", "其他入库单", null, 67L, 2));
        menus.add(menu(77L, "OtherOutboundList", "其他出库单", null, 67L, 3));
        menus.add(menu(86L, "CostAdjustmentList", "成本调整单", null, 67L, 4));
        menus.add(menu(211L, "AssemblyOrderList", "组装拆卸单", null, 67L, 5));
        menus.add(disabledMenu(246L, "LossOrderList", "报损单", null, 67L, 6));
        menus.add(disabledMenu(247L, "GainOrderList", "报溢单", null, 67L, 7));
        // 库存增强
        menus.add(menu(212L, null, "库存增强", null, 27L, 1));
        menus.add(menu(213L, "ComingSoon", "批次管理", null, 212L, 0));
        menus.add(menu(214L, "ShelfLifeList", "保质期管理", null, 212L, 1));
        menus.add(menu(215L, "SerialManagement", "序列号管理", null, 212L, 2));
        // 库存报表
        menus.add(menu(68L, null, "库存报表", null, 27L, 2));
        menus.add(menu(72L, "InventoryReport", "库存余额表", null, 68L, 0));
        menus.add(menu(248L, "InventoryOverview", "库存状况（总览）", null, 68L, 1));
        menus.add(menu(73L, "InventoryItemReport", "进销存明细表", null, 68L, 2));
        menus.add(menu(74L, "InventorySummaryReport", "进销存汇总表", null, 68L, 3));
        menus.add(menu(87L, "InventoryCostBatchList", "库存批次表", null, 68L, 4));
        menus.add(menu(249L, "InventoryDistribution", "库存分布（按仓库/门店）", null, 68L, 5));
        menus.add(menu(250L, "VirtualStock", "虚拟库存状况表", null, 68L, 6));
        menus.add(menu(251L, "BatchTracking", "批次跟踪查询", null, 68L, 7));
        menus.add(menu(216L, "InventoryWarning", "库存预警", null, 68L, 8));
        menus.add(menu(256L, "InventoryOverstock", "库存上限预警", null, 68L, 9));
        menus.add(menu(252L, "StockTakeList", "盘点记录查询", null, 68L, 10));
        menus.add(menu(253L, "LossGainReport", "报损报溢汇总表", null, 68L, 11));
        menus.add(menu(254L, "TransferReport", "调拨统计报表", null, 68L, 12));

        // ===== 资金管理 (根菜单) =====
        menus.add(menu(28L, "Fund", "资金管理", "wallet", null, 3));
        // 资金单据（按使用频率排序）
        menus.add(menu(29L, null, "资金单据", null, 28L, 0));
        menus.add(menu(30L, "OrderReceiptList", "收款单", null, 29L, 0));
        menus.add(menu(31L, "OrderPaymentList", "付款单", null, 29L, 1));
        menus.add(menu(32L, "VerificationList", "核销单", null, 29L, 2));
        menus.add(menu(217L, "AdvanceReceiptList", "预收款单", null, 29L, 3));
        menus.add(menu(218L, "AdvancePaymentList", "预付款单", null, 29L, 4));
        menus.add(menu(88L, "SettlementList", "结算单", null, 29L, 5));
        menus.add(menu(33L, "AccountTransferList", "资金转账", null, 29L, 6));
        menus.add(menu(34L, "OtherReceiptList", "其他收入单", null, 29L, 7));
        menus.add(menu(35L, "OtherExpenseList", "其他支出单", null, 29L, 8));
        // 资金报表
        menus.add(menu(36L, null, "资金报表", null, 28L, 1));
        menus.add(disabledMenu(38L, "CustomerStatements", "客户对账单", null, 36L, 0));
        menus.add(disabledMenu(39L, "VendorStatements", "供货商对账单", null, 36L, 1));
        menus.add(menu(40L, "CustomerFlowReport", "应收账款明细表", null, 36L, 2));
        menus.add(menu(41L, "SupplierFlowReport", "应付账款明细表", null, 36L, 3));
        menus.add(menu(37L, "CounterpartDebt", "往来单位欠款表", null, 36L, 4));
        menus.add(menu(219L, "ProfitReport", "利润表", null, 36L, 5));
        menus.add(menu(220L, "AccountFlowList", "资金流水", null, 36L, 6));
        menus.add(menu(255L, "AdvanceBalanceReport", "预收预付余额", null, 36L, 7));
        // 资金增强
        menus.add(menu(221L, null, "资金增强", null, 28L, 2));
        menus.add(menu(222L, "ComingSoon", "信用额度", null, 221L, 0));

        // ===== 发票管理 (根菜单) =====
        menus.add(menu(223L, "Invoice", "发票管理", "bill", null, 4));
        // 开票管理
        menus.add(menu(224L, null, "开票管理", null, 223L, 0));
        menus.add(menu(225L, "InvoiceIssue", "蓝字发票开具", null, 224L, 0));
        menus.add(menu(226L, "InvoiceManagement", "红字发票", null, 224L, 1));
        menus.add(menu(227L, "ComingSoon", "批量开票", null, 224L, 2));
        // 发票查询
        menus.add(menu(228L, null, "发票查询", null, 223L, 1));
        menus.add(menu(229L, "InvoiceManagement", "发票查询", null, 228L, 0));
        // 发票报表
        menus.add(menu(230L, null, "发票报表", null, 223L, 2));
        menus.add(menu(231L, "ComingSoon", "开票统计", null, 230L, 0));
        menus.add(menu(232L, "ComingSoon", "剩余额度", null, 230L, 1));
        // 发票归集
        menus.add(menu(233L, null, "发票归集", null, 223L, 3));
        menus.add(menu(234L, "InvoiceAggregation", "销项归集", null, 233L, 0));
        menus.add(menu(235L, "InvoiceAggregation", "进项归集", null, 233L, 1));

        // ===== 基础资料 (根菜单) =====
        menus.add(menu(1L, "basic", "基础资料", "root-list", null, 5));
        // 商品管理
        menus.add(menu(6L, null, "商品管理", null, 1L, 0));
        menus.add(menu(20L, "ProductList", "商品档案", null, 6L, 0));
        menus.add(menu(236L, "ProductCategoryList", "商品分类", null, 6L, 1));
        menus.add(menu(80L, "UnitList", "计量单位", null, 6L, 2));
        menus.add(menu(44L, "ProductPriceList", "产品价格资料", null, 6L, 3));
        menus.add(menu(45L, "PriceRecordList", "产品价格记录", null, 6L, 4));
        menus.add(menu(81L, "PricingPolicyList", "价格取数规则", null, 6L, 5));
        menus.add(menu(237L, "ProductComboList", "商品套餐", null, 6L, 6));
        menus.add(menu(238L, "BrandList", "品牌管理", null, 6L, 7));
        menus.add(menu(239L, "ProductAttributeList", "辅助属性", null, 6L, 8));
        // 往来单位
        menus.add(menu(10L, null, "往来单位", null, 1L, 1));
        menus.add(menu(7L, "CustomerList", "客户档案", null, 10L, 0));
        menus.add(menu(8L, "SupplierList", "供应商档案", null, 10L, 1));
        menus.add(menu(12L, "CustomerLevelList", "客户等级", null, 10L, 2));
        menus.add(menu(240L, "ComingSoon", "信用额度", null, 10L, 3));
        // 基础档案
        menus.add(menu(82L, null, "基础档案", null, 1L, 2));
        menus.add(menu(9L, "WarehouseList", "仓库管理", null, 82L, 0));
        menus.add(menu(79L, "AccountList", "账户管理", null, 82L, 1));
        menus.add(menu(23L, "AccountTypeList", "收支类型", null, 82L, 2));
        menus.add(menu(24L, "PaymentMethodList", "结算方式", null, 82L, 3));
        // 期初录入
        menus.add(menu(43L, null, "期初录入", null, 1L, 3));
        menus.add(menu(83L, "InventoryInitialList", "库存期初录入", null, 43L, 0));
        menus.add(menu(84L, "CustomerInitialList", "客户期初录入", null, 43L, 1));
        menus.add(menu(85L, "SupplierInitialList", "货商期初录入", null, 43L, 2));

        // ===== 系统设置 (根菜单) =====
        menus.add(menu(14L, "Setting", "系统设置", "setting", null, 6));
        // 系统设置
        menus.add(menu(15L, null, "系统设置", null, 14L, 0));
        menus.add(menu(16L, "MerchantInfo", "企业信息", null, 15L, 0));
        menus.add(menu(17L, "AccountBookList", "账套管理", null, 15L, 1));
        menus.add(menu(18L, "RoleList", "角色权限", null, 15L, 2));
        menus.add(menu(19L, "AdminList", "员工账号", null, 15L, 3));
        menus.add(menu(46L, "CodeRuleList", "编码规则", null, 15L, 4));
        menus.add(menu(47L, "CheckoutList", "结账/反结账", null, 15L, 5));
        menus.add(menu(48L, "PrintTemplateList", "打印模板", null, 15L, 6));
        menus.add(menu(49L, "BackupRestore", "备份与恢复", null, 15L, 7));
        menus.add(menu(50L, "SystemLogList", "操作日志", null, 15L, 8));
        // 发票设置
        menus.add(menu(241L, null, "发票设置", null, 14L, 1));
        menus.add(menu(242L, "TaxConfig", "税号配置", null, 241L, 0));
        menus.add(menu(243L, "DigitalAccount", "电子税务局账号", null, 241L, 1));
        menus.add(menu(244L, "ServiceItems", "服务项目", null, 241L, 2));

        // ===== 已废弃菜单（禁用）=====
        menus.add(disabledMenu(200L, "QuickOrder", "快速开单", null, 58L, 5));
        menus.add(disabledMenu(201L, null, "销售增强", null, 26L, 2));
        menus.add(disabledMenu(202L, "SalesDrivenPurchase", "以销定购看板", null, 201L, 0));
        menus.add(disabledMenu(207L, null, "采购增强", null, 25L, 2));
        menus.add(disabledMenu(208L, "SalesDrivenPurchase", "以销定购看板", null, 51L, 6));
        menus.add(disabledMenu(259L, null, "以销定购", null, 26L, 3));
        menus.add(disabledMenu(245L, "ComingSoon", "开票默认项", null, 241L, 3));

        return menus;
    }

    private Menu menu(Long id, String component, String name, String iconCls, Long parentId, Integer pos) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setComponent(component);
        menu.setName(name);
        menu.setIconCls(iconCls);
        menu.setParentId(parentId);
        menu.setPos(pos);
        menu.setEnabled(true);
        menu.setMenuType(Menu.MenuType.MENU);
        menu.setMenuModule(Menu.MenuModule.MERCHANT);
        menu.setMenuGroup(Menu.MenuGroup.MERCHANT);
        return menu;
    }

    private Menu disabledMenu(Long id, String component, String name, String iconCls, Long parentId, Integer pos) {
        Menu menu = menu(id, component, name, iconCls, parentId, pos);
        menu.setEnabled(false);
        return menu;
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (StrUtil.isNotEmpty(name)) {
                builder.and(qMerchant.name.contains(name));
            }
        }
    }
}
