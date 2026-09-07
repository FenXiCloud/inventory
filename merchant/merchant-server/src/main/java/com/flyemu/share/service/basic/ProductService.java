package com.flyemu.share.service.basic;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.querydsl.BlazeJPAQuery;
import com.flyemu.share.common.PinYinUtil;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.AuxiliaryUnitPrice;
import com.flyemu.share.dto.ProductDto;
import com.flyemu.share.dto.ProductImportVo;
import com.flyemu.share.dto.SelectProductDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.sales.SalesOrder;
import com.flyemu.share.entity.sales.SalesOrderItem;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.ProductForm;
import com.flyemu.share.repository.basic.CategoryTreeRepository;
import com.flyemu.share.repository.basic.CustomerLevelPriceRepository;
import com.flyemu.share.repository.basic.CustomerLevelRepository;
import com.flyemu.share.repository.basic.UnitRepository;
import com.flyemu.share.repository.inventory.InventoryItemRepository;
import com.flyemu.share.repository.basic.ProductRepository;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.inventory.*;
import com.flyemu.share.service.purchase.PurchaseInboundService;
import com.flyemu.share.service.purchase.PurchaseOrderService;
import com.flyemu.share.service.purchase.PurchaseReturnService;
import com.flyemu.share.service.sales.SalesOrderService;
import com.flyemu.share.service.sales.SalesOutboundService;
import com.flyemu.share.service.sales.SalesReturnService;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.flyemu.share.way.ProductExistenceChecker;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAUpdateClause;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.rowset.serial.SerialException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService extends BaseService {

    private final static QProduct qProduct = QProduct.product;
    private final static QProductCategory qProductCategory = QProductCategory.productCategory;
    private final static QCustomerLevel qCustomerLevel = QCustomerLevel.customerLevel;
    private final static QInventory qInventory = QInventory.inventory;

    private final QUnit qUnit = QUnit.unit;
    private final QCustomerLevelPrice qCustomerLevelPrice = QCustomerLevelPrice.customerLevelPrice;

    private final ProductRepository productRepository;
    private final CategoryTreeRepository categoryTreeRepository;
    private final UnitRepository unitRepository;
    private final CustomerLevelPriceRepository customerLevelPriceRepository;
    private final CustomerLevelRepository customerLevelRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PriceRecordService priceRecordService;
    private final PriceResolveService priceResolveService;
    private final CodeRuleService codeRuleService;
    private final ProductCategoryService productCategoryService;
    private final ProductExistenceChecker existenceChecker;
    //连表分页查询
    public PageResults<ProductDto> query(Page page, Query query) {
        // 选中某个分类时，展示该分类及其下所有下级分类的产品（商品挂末级，父分类需聚合子分类）
        Long categoryId = query.productCategoryId;
        if (categoryId != null) {
            query.productCategoryId = null; // 让 builders() 不再按“直属分类”精确过滤
        }
        BooleanBuilder where = query.builders();
        if (categoryId != null) {
            List<Long> subCategoryIds = categoryTreeRepository.getAllSubCategoryIds(categoryId);
            if (CollUtil.isNotEmpty(subCategoryIds)) {
                where.and(qProduct.productCategoryId.in(subCategoryIds));
            }
        }
        PagedList<Tuple> pagedList = bqf.selectFrom(qProduct).select(qProduct, qUnit.name, qProductCategory.name).leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId)).leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId)).where(where).orderBy(qProduct.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());
        ArrayList<ProductDto> collect = pagedList.stream().collect(ArrayList::new, (list, tuple) -> {
            ProductDto dto = BeanUtil.toBean(tuple.get(qProduct), ProductDto.class);
            dto.setProductCategoryName(tuple.get(qProductCategory.name));
            dto.setUnitName(tuple.get(qUnit.name));
            list.add(dto);
        }, List::addAll);
        return new PageResults<>(collect, page, pagedList.getTotalSize());
    }

    @Transactional
    public void save(ProductForm productForm, Long merchantId, Long accountBookId) {
        Product product = productForm.getProduct();
        productCategoryService.assertCanBindProduct(product.getProductCategoryId(), merchantId, accountBookId);
        if (product.getEnableMultiUnit()) {
            Assert.isTrue(CollUtil.isNotEmpty(product.getAuxiliaryUnitPrices()), "开启多单位,必须选择一个副单位");
            Set<Long> checkUnit = new HashSet<>();
            checkUnit.add(product.getUnitId());
            for (AuxiliaryUnitPrice up : product.getAuxiliaryUnitPrices()) {
                if (up.getUnitId() != null) {
                    Assert.isFalse(checkUnit.contains(up.getUnitId()), up.getUnitName() + "单位,不能一致");
                }
            }
        }
        Long previousCategoryId = null;
        if (product.getId() != null) {
            Product original = productRepository.getById(product.getId());
            previousCategoryId = original.getProductCategoryId();

            if (original.getEnableMultiUnit()) {
                List<Long> unitIds;
                if (!product.getEnableMultiUnit()) { //关闭多单位需要去检测
                    unitIds = original.getAuxiliaryUnitPrices().stream().map(AuxiliaryUnitPrice::getUnitId).collect(Collectors.toList());
                } else {//取有变动的单位去校验
                    unitIds = new ArrayList<>();
                    if (!original.getAuxiliaryUnitPrices().equals(product.getAuxiliaryUnitPrices())) {
                        List<AuxiliaryUnitPrice> prices = product.getAuxiliaryUnitPrices();
                        original.getAuxiliaryUnitPrices().forEach(item -> {
                            if (!prices.contains(item)) {
                                unitIds.add(item.getUnitId());
                            }
                        });
                    }
                }
            }
            if (!original.getName().equals(product.getName())) {
                original.setPinyin(PinYinUtil.getFirstLettersLo(product.getName()) + "," + PinYinUtil.getPinyinString(product.getName()));
            }
            BeanUtil.copyProperties(product, original, CopyOptions.create().ignoreNullValue());
            //保存价格记录
            savePrice(original);
            product = productRepository.save(original);

        } else {
            if (StringUtils.isEmpty(product.getCode())) {
                CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(
                        CodeRule.DocumentType.商品,
                        merchantId,
                        accountBookId
                );

                if (codeRule != null) {
                    StringBuilder codeBuilder = new StringBuilder();
                    if (StrUtil.isNotBlank(codeRule.getPrefix())) {
                        codeBuilder.append(codeRule.getPrefix());
                    }
                    if (StrUtil.isNotBlank(codeRule.getFormat())) {
                        String formattedDate = DateUtil.format(LocalDateTime.now(), codeRule.getFormat());
                        codeBuilder.append(formattedDate);
                    }
                    Integer serialLength = codeRule.getSerialNumberLength();
                    if (serialLength != null && serialLength > 0) {
                        JPAQuery<Long> query = jqf.select(qProduct.id.count())
                                .from(qProduct)
                                .where(
                                        qProduct.merchantId.eq(merchantId)
                                                .and(qProduct.accountBookId.eq(accountBookId))
                                );
                        Long count = query.fetchOne();
                        Integer currentSerial = Math.toIntExact(count != null ? count + 1 : 1L);
                        String serialStr = String.format("%0" + serialLength + "d", currentSerial);
                        codeBuilder.append(serialStr);
                    }

                    product.setCode(codeBuilder.toString());

                } else {
                    product.setCode(CodeGenerator.generateCode());
                }
            }else{
                Long count = jqf.select(qProduct.id.count())
                        .from(qProduct)
                        .where(qProduct.code.eq(product.getCode())
                                .and(qProduct.merchantId.eq(merchantId))
                                .and(qProduct.accountBookId.eq(accountBookId)))
                        .fetchOne();

                if (count != null && count > 0) {
                    throw new ServiceException("编码已存在，请重新输入！");
                }

            }
            product.setAccountBookId(accountBookId);
            product.setMerchantId(merchantId);
            product.setEnabled(true);
            product.setPinyin(PinYinUtil.getFirstLettersLo(product.getName()) + "," + PinYinUtil.getPinyinString(product.getName()));
            productRepository.save(product);
        }

        if (CollUtil.isNotEmpty(productForm.getCustomerLevelPriceList())) {
            List<CustomerLevelPrice> cps = new ArrayList<>();
            for (JSONObject cp : productForm.getCustomerLevelPriceList()) {
                CustomerLevelPrice levelPrice = new CustomerLevelPrice();
                levelPrice.setProductId(product.getId());
                levelPrice.setUnitId(product.getUnitId());
                levelPrice.setPrice(cp.getBigDecimal("price"));
                levelPrice.setMerchantId(merchantId);
                levelPrice.setAccountBookId(accountBookId);
                levelPrice.setCustomerLevelId(cp.getLong("customerLeveId"));
                if (product.getEnableMultiUnit()) {
                    List<AuxiliaryUnitPrice> ups = new ArrayList<>();
                    for (AuxiliaryUnitPrice multiUnit : product.getAuxiliaryUnitPrices()) {
                        AuxiliaryUnitPrice up = new AuxiliaryUnitPrice();
                        up.setUnitName(multiUnit.getUnitName());
                        up.setUnitId(multiUnit.getUnitId());
                        up.setUnitPrice(cp.getBigDecimal(multiUnit.getUnitId() + ""));
                        ups.add(up);
                    }
                    levelPrice.setAuxiliaryUnitPrices(ups);
                }
                //保存价格记录
                savePrice(levelPrice);
                cps.add(levelPrice);
            }
            jqf.delete(qCustomerLevelPrice).where(qCustomerLevelPrice.productId.eq(product.getId()).and(qCustomerLevelPrice.merchantId.eq(merchantId)).and(qCustomerLevelPrice.accountBookId.eq(accountBookId))).execute();
            customerLevelPriceRepository.saveAll(cps);
        } else {
            List<CustomerLevelPrice> priceList = jqf.selectFrom(qCustomerLevelPrice).where(qCustomerLevelPrice.productId.eq(product.getId()).and(qCustomerLevelPrice.merchantId.eq(merchantId)).and(qCustomerLevelPrice.accountBookId.eq(accountBookId))).fetch();
            for (CustomerLevelPrice price : priceList) {
                price.setUnitId(product.getUnitId());
                if (product.getEnableMultiUnit()) {
                    for (int i = 0; i < product.getAuxiliaryUnitPrices().size(); i++) {
                        AuxiliaryUnitPrice gp = product.getAuxiliaryUnitPrices().get(i);
                        if (CollUtil.isNotEmpty(price.getAuxiliaryUnitPrices())) {
                            AuxiliaryUnitPrice auxiliaryUnitPrice = null;
                            if (price.getAuxiliaryUnitPrices() != null && price.getAuxiliaryUnitPrices().size() > i) {
                                auxiliaryUnitPrice = price.getAuxiliaryUnitPrices().get(i);
                            }
                            if (auxiliaryUnitPrice != null) {
                                auxiliaryUnitPrice.setUnitId(gp.getUnitId());
                                auxiliaryUnitPrice.setUnitName(gp.getUnitName());
                                auxiliaryUnitPrice.setConversionRate(gp.getConversionRate());
                            } else {
                                AuxiliaryUnitPrice up = new AuxiliaryUnitPrice();
                                up.setUnitId(gp.getUnitId());
                                up.setUnitName(gp.getUnitName());
                                up.setConversionRate(gp.getConversionRate());
                                up.setUnitPrice(BigDecimal.ZERO);
                                price.getAuxiliaryUnitPrices().add(up);
                            }
                        } else {
                            List<AuxiliaryUnitPrice> ups = new ArrayList<>();
                            for (AuxiliaryUnitPrice multiUnit : product.getAuxiliaryUnitPrices()) {
                                AuxiliaryUnitPrice up = new AuxiliaryUnitPrice();
                                up.setUnitName(multiUnit.getUnitName());
                                up.setUnitId(multiUnit.getUnitId());
                                up.setConversionRate(gp.getConversionRate());
                                up.setUnitPrice(BigDecimal.ZERO);
                                ups.add(up);
                            }
                            price.setAuxiliaryUnitPrices(ups);
                        }
                    }
                }
            }
            if (CollUtil.isNotEmpty(priceList)) {
                customerLevelPriceRepository.saveAll(priceList);
            }
        }
        // 初始化期初余额
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setProductId(product.getId());
        inventoryItem.setMerchantId(merchantId);
        inventoryItem.setAccountBookId(accountBookId);
        inventoryItem.setOperationType(OperationType.期初余额);
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(-1L);
        inventoryItem.setFirstSort(true);
        inventoryItemRepository.save(inventoryItem);

        // 末级状态按分类下是否有产品刷新
        productCategoryService.refreshLeafByProducts(product.getProductCategoryId(), merchantId, accountBookId);
        if (previousCategoryId != null && !previousCategoryId.equals(product.getProductCategoryId())) {
            productCategoryService.refreshLeafByProducts(previousCategoryId, merchantId, accountBookId);
        }
    }

    /**
     * 采购订单/采购入库单录入时，商品档案中不存在该商品则按名称快速建档：
     * 编码自动生成、单位取第一个计量单位、分类取第一个末级分类、预计进货价默认 0。
     */
    @Transactional
    public SelectProductDto quickCreate(String name, String unitName, Long productCategoryId, Long merchantId, Long accountBookId) {
        Assert.notBlank(name, "商品名称不能为空");
        String trimmed = name.trim();

        // 获取或创建单位
        Unit unit;
        if (StrUtil.isNotBlank(unitName)) {
            // 查找或创建单位
            unit = jqf.selectFrom(qUnit)
                    .where(qUnit.merchantId.eq(merchantId)
                            .and(qUnit.accountBookId.eq(accountBookId))
                            .and(qUnit.name.eq(unitName.trim())))
                    .fetchFirst();
            if (unit == null) {
                // 创建新单位
                unit = new Unit();
                unit.setName(unitName.trim());
                unit.setMerchantId(merchantId);
                unit.setAccountBookId(accountBookId);
                unit = unitRepository.save(unit);
            }
        } else {
            unit = jqf.selectFrom(qUnit)
                    .where(qUnit.merchantId.eq(merchantId).and(qUnit.accountBookId.eq(accountBookId)))
                    .orderBy(qUnit.id.asc())
                    .fetchFirst();
            Assert.notNull(unit, "请先到「基本资料-单位」维护计量单位");
        }

        // 获取或创建类别
        ProductCategory category;
        if (productCategoryId != null) {
            category = jqf.selectFrom(qProductCategory)
                    .where(qProductCategory.id.eq(productCategoryId)
                            .and(qProductCategory.merchantId.eq(merchantId))
                            .and(qProductCategory.accountBookId.eq(accountBookId)))
                    .fetchFirst();
            Assert.notNull(category, "产品类别不存在");
        } else {
            List<ProductCategory> categories = jqf.selectFrom(qProductCategory)
                    .where(qProductCategory.merchantId.eq(merchantId).and(qProductCategory.accountBookId.eq(accountBookId)))
                    .orderBy(qProductCategory.id.asc())
                    .fetch();
            Assert.notEmpty(categories, "请先到「基本资料-产品分类」维护分类");
            Set<Long> parentIds = categories.stream().map(ProductCategory::getPid).filter(Objects::nonNull).collect(Collectors.toSet());
            category = categories.stream().filter(c -> !parentIds.contains(c.getId())).findFirst().orElse(categories.get(0));
        }

        Product product = new Product();
        product.setName(trimmed);
        product.setUnitId(unit.getId());
        product.setProductCategoryId(category.getId());
        product.setPurchasePrice(BigDecimal.ZERO);
        product.setEnableMultiUnit(false);
        product.setEnabled(true);

        ProductForm form = new ProductForm();
        form.setProduct(product);
        save(form, merchantId, accountBookId);

        return buildSelectProductDto(product, unit.getName(), category.getName());
    }

    private SelectProductDto buildSelectProductDto(Product product, String unitName, String categoryName) {
        SelectProductDto dto = new SelectProductDto();
        dto.setProductId(product.getId());
        dto.setImgPath(product.getImgPath());
        dto.setProductCode(product.getCode());
        dto.setProductName(product.getName());
        dto.setSpec(product.getSpecification());
        dto.setUnitId(product.getUnitId());
        dto.setUnitName(unitName);
        dto.setCategoryName(categoryName);
        dto.setPrice(product.getPurchasePrice());
        dto.setAuxiliaryUnitPrices(product.getAuxiliaryUnitPrices());
        dto.setTitle();
        return dto;
    }

    /**
     * 批量导入商品
     *
     * @param rows          导入行
     * @param merchantId    商户
     * @param accountBookId 账套
     */
    @Transactional
    public void importData(List<ProductImportVo> rows, Long merchantId, Long accountBookId) {
        // 基础校验
        for (int i = 0; i < rows.size(); i++) {
            ProductImportVo row = rows.get(i);
            if (StrUtil.isBlank(row.getName())) {
                throw new ServiceException("第" + (i + 2) + "行：商品名称不能为空");
            }
            if (StrUtil.isBlank(row.getProductCategoryName())) {
                throw new ServiceException("第" + (i + 2) + "行：分类不能为空");
            }
            if (StrUtil.isBlank(row.getUnitName())) {
                throw new ServiceException("第" + (i + 2) + "行：单位不能为空");
            }
        }

        // 文件内编码去重
        Set<String> codeSet = new HashSet<>();
        List<String> duplicateCodes = rows.stream()
                .map(ProductImportVo::getCode)
                .filter(c -> StrUtil.isNotBlank(c) && !codeSet.add(c))
                .distinct()
                .toList();
        Assert.isTrue(duplicateCodes.isEmpty(), "导入数据中存在重复的商品编码：" + String.join("、", duplicateCodes));

        // 已存在编码校验
        List<String> existingCodes = jqf.select(qProduct.code)
                .from(qProduct)
                .where(qProduct.merchantId.eq(merchantId).and(qProduct.accountBookId.eq(accountBookId)))
                .fetch();
        Set<String> existingCodeSet = new HashSet<>(existingCodes);
        List<String> duplicatedInDb = rows.stream()
                .map(ProductImportVo::getCode)
                .filter(c -> StrUtil.isNotBlank(c) && existingCodeSet.contains(c))
                .distinct()
                .toList();
        Assert.isTrue(duplicatedInDb.isEmpty(), "以下商品编码已存在：" + String.join("、", duplicatedInDb));

        for (ProductImportVo row : rows) {
            ProductCategory category = jqf.selectFrom(qProductCategory)
                    .where(qProductCategory.merchantId.eq(merchantId)
                            .and(qProductCategory.accountBookId.eq(accountBookId))
                            .and(qProductCategory.name.eq(row.getProductCategoryName())))
                    .fetchFirst();
            if (category == null) {
                throw new ServiceException("分类不存在：" + row.getProductCategoryName());
            }
            Unit unit = jqf.selectFrom(qUnit)
                    .where(qUnit.merchantId.eq(merchantId)
                            .and(qUnit.accountBookId.eq(accountBookId))
                            .and(qUnit.name.eq(row.getUnitName())))
                    .fetchFirst();
            if (unit == null) {
                throw new ServiceException("单位不存在：" + row.getUnitName());
            }

            Product product = new Product();
            product.setCode(StrUtil.isNotBlank(row.getCode()) ? row.getCode().trim() : generateProductCode(merchantId, accountBookId));
            product.setName(row.getName());
            product.setBrand(row.getBrand());
            product.setSpecification(row.getSpecification());
            product.setUnitId(unit.getId());
            product.setProductCategoryId(category.getId());
            product.setPurchasePrice(row.getPurchasePrice() != null ? row.getPurchasePrice() : BigDecimal.ZERO);
            product.setAlertQuantity(row.getAlertQuantity());
            product.setRemarks(row.getRemarks());
            product.setEnabled(true);
            product.setEnableMultiUnit(false);
            product.setAccountBookId(accountBookId);
            product.setMerchantId(merchantId);
            product.setPinyin(PinYinUtil.getFirstLettersLo(product.getName()) + "," + PinYinUtil.getPinyinString(product.getName()));
            productRepository.save(product);

            // 初始化期初余额
            InventoryItem inventoryItem = new InventoryItem();
            inventoryItem.setProductId(product.getId());
            inventoryItem.setMerchantId(merchantId);
            inventoryItem.setAccountBookId(accountBookId);
            inventoryItem.setOperationType(OperationType.期初余额);
            inventoryItem.setCreatedAt(LocalDateTime.now());
            inventoryItem.setCreatedBy(-1L);
            inventoryItem.setFirstSort(true);
            inventoryItemRepository.save(inventoryItem);

            // 末级状态按分类下是否有产品刷新
            productCategoryService.refreshLeafByProducts(category.getId(), merchantId, accountBookId);
        }
    }

    /**
     * 生成商品编码（与保存逻辑一致：编码规则优先，否则随机）
     */
    private String generateProductCode(Long merchantId, Long accountBookId) {
        CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(
                CodeRule.DocumentType.商品, merchantId, accountBookId);
        if (codeRule == null) {
            return CodeGenerator.generateCode();
        }
        StringBuilder codeBuilder = new StringBuilder();
        if (StrUtil.isNotBlank(codeRule.getPrefix())) {
            codeBuilder.append(codeRule.getPrefix());
        }
        if (StrUtil.isNotBlank(codeRule.getFormat())) {
            codeBuilder.append(DateUtil.format(LocalDateTime.now(), codeRule.getFormat()));
        }
        Integer serialLength = codeRule.getSerialNumberLength();
        if (serialLength != null && serialLength > 0) {
            Long count = jqf.select(qProduct.id.count())
                    .from(qProduct)
                    .where(qProduct.merchantId.eq(merchantId).and(qProduct.accountBookId.eq(accountBookId)))
                    .fetchOne();
            codeBuilder.append(String.format("%0" + serialLength + "d", (count != null ? count : 0) + 1));
        }
        return codeBuilder.toString();
    }

    /**
     * 保存预计采购价格
     *
     * @param product
     */
    private void savePrice(Product product) {
        //保存价格记录
        PriceRecord priceRecord = new PriceRecord();
        priceRecord.setUnitPrice(product.getPurchasePrice());
        priceRecord.setBaseUnitId(product.getUnitId());
        priceRecord.setProductId(product.getId());
        priceRecord.setMerchantId(product.getMerchantId());
        priceRecord.setAccountBookId(product.getAccountBookId());
        priceRecord.setPriceSource(PriceSource.商品价格资料);
        priceRecord.setPriceType(PriceType.预计采购价格);
        priceRecordService.savePriceRecord(priceRecord);
    }

    /**
     * 保存客户等级价格
     *
     * @param customerLevelPrice
     */
    private void savePrice(CustomerLevelPrice customerLevelPrice) {
        //保存价格记录
        PriceRecord priceRecord = new PriceRecord();
        priceRecord.setUnitPrice(customerLevelPrice.getPrice());
        priceRecord.setBaseUnitId(customerLevelPrice.getUnitId());
        priceRecord.setProductId(customerLevelPrice.getProductId());
        priceRecord.setMerchantId(customerLevelPrice.getMerchantId());
        priceRecord.setAccountBookId(customerLevelPrice.getAccountBookId());
        priceRecord.setPriceSource(PriceSource.商品价格资料);
        Long customerLevelId = customerLevelPrice.getCustomerLevelId();
        CustomerLevel customerLevel = customerLevelRepository.getById(customerLevelId);
        String name = customerLevel.getName();

        PriceType priceType = null;
        if (StringUtils.equals(name, "会员价")) {
            priceType = PriceType.VIP客户价格;
        } else if (StringUtils.equals(name, "零售价")) {
            priceType = PriceType.零售客户价格;
        } else {
            log.info("客户等级价格保存失败：{}", name);
            return;
        }
        priceRecord.setPriceType(priceType);
        priceRecordService.savePriceRecord(priceRecord);
    }

    @Transactional
    public void delete(Long productId, Long merchantId, Long accountBookId) {
        if (existenceChecker.existsInPurchaseOrder(productId, 1)) {
            throw new ServiceException("该商品已存在采购单,不能删除");
        }
        if (existenceChecker.existsInPurchaseInbound(productId, 1)) {
            throw new ServiceException("该商品已存在采购入库单,不能删除");
        }
        if (existenceChecker.existsInPurchaseReturn(productId, 1)) {
            throw new ServiceException("该商品已存在采购退货单,不能删除");
        }
        if (existenceChecker.existsInSalesOrder(productId, 1)) {
            throw new ServiceException("该商品已存在销售单,不能删除");
        }
        if (existenceChecker.existsInSalesOutbound(productId, 1)) {
            throw new ServiceException("该商品已存在销售出库单,不能删除");
        }
        if (existenceChecker.existsInSalesReturn(productId, 1)) {
            throw new ServiceException("该商品已存在销售退货单,不能删除");
        }
        if (existenceChecker.existsInInventoryTransfer(productId, 1)) {
            throw new ServiceException("该商品已存在库存调拨单,不能删除");
        }
        if (existenceChecker.existsInStockTake(productId, 1)) {
            throw new ServiceException("该商品已存在库存盘点单,不能删除");
        }
        if (existenceChecker.existsInOtherInbound(productId, 1)) {
            throw new ServiceException("该商品已存在其他入库单,不能删除");
        }
        if (existenceChecker.existsInOtherOutbound(productId, 1)) {
            throw new ServiceException("该商品已存在其他出库单,不能删除");
        }
        if (existenceChecker.existsInCostAdjustment(productId, 1)) {
            throw new ServiceException("该商品已存在成本调整单,不能删除");
        }

        Product deleting = productRepository.getById(productId);
        Long categoryId = deleting.getProductCategoryId();
        jqf.delete(qCustomerLevelPrice).where(qCustomerLevelPrice.productId.eq(productId).and(qCustomerLevelPrice.merchantId.eq(merchantId)).and(qCustomerLevelPrice.accountBookId.eq(accountBookId))).execute();
        jqf.delete(qProduct).where(qProduct.id.eq(productId).and(qProduct.merchantId.eq(merchantId)).and(qProduct.accountBookId.eq(accountBookId))).execute();
        productCategoryService.refreshLeafByProducts(categoryId, merchantId, accountBookId);
    }

    public List<ProductDto> select(Long merchantId, Long accountBookId, Long productCategoryId, Long warehouseId, Long customerId) {
        priceResolveService.ensureDefaultPolicies(merchantId, accountBookId);
        BlazeJPAQuery<Tuple> where = bqf.selectFrom(qProduct).
                select(qProduct, qUnit.name, qProductCategory.name).
                leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId));
        if (warehouseId != null) {
            where.innerJoin(qInventory)
                    .on(qInventory.productId.eq(qProduct.id)
                            .and(qInventory.warehouseId.eq(warehouseId)))
                    .where(qInventory.currentQuantity.gt(0));
        }
        where.where(qProduct.merchantId.eq(merchantId)
                .and(qProduct.accountBookId.eq(accountBookId)).and(qProduct.enabled.isTrue()));
        if (productCategoryId != null) {
            where.where(qProduct.productCategoryId.eq(productCategoryId));
        }
        List<Tuple> fetch = where.fetch();
        ArrayList<ProductDto> result = fetch.stream().collect(ArrayList::new, (list, tuple) -> {
            ProductDto dto = BeanUtil.toBean(tuple.get(qProduct), ProductDto.class);
            dto.setUnitName(tuple.get(qUnit.name));
            dto.setProductCategoryName(tuple.get(qProductCategory.name));
            List<CustomerLevelPrice> customerLevelPrices = jqf.selectFrom(qCustomerLevelPrice)
                    .where(qCustomerLevelPrice.productId.eq(dto.getId())
                            .and(qCustomerLevelPrice.merchantId.eq(merchantId))
                            .and(qCustomerLevelPrice.accountBookId.eq(accountBookId)))
                    .orderBy(qCustomerLevelPrice.id.desc())
                    .fetch();
            dto.setCustomerLevelPriceList(customerLevelPrices);
            dto.setLastSalePrice(priceResolveService.resolveSalesPrice(dto.getId(), customerId, merchantId, accountBookId));
            // 多单位商品补基座单位(rate=1)到单位下拉，供销售订单/销售出库等表单行内切换单位（防御性拷贝，不改写持久化 JSON）
            List<AuxiliaryUnitPrice> aux = dto.getAuxiliaryUnitPrices();
            if (Boolean.TRUE.equals(dto.getEnableMultiUnit()) && dto.getUnitId() != null && CollUtil.isNotEmpty(aux)) {
                dto.setAuxiliaryUnitPrices(ProductAuxiliaryUnitService.withBase(
                        Long.valueOf(dto.getUnitId()), dto.getUnitName(), dto.getLastSalePrice(), aux));
            }
            list.add(dto);
        }, List::addAll);
        return result;
    }

    public Product loadById(Long productId, Long merchantId) {
        return jqf.selectFrom(qProduct).where(qProduct.id.eq(productId).and(qProduct.merchantId.eq(merchantId))).fetchFirst();
    }

    public Map<Long, CustomerLevelPrice> customerLevelPrice(Long productId, Long merchantId, Long accountBookId) {
        return jqf.selectFrom(qCustomerLevelPrice).where(qCustomerLevelPrice.productId.eq(productId).and(qCustomerLevelPrice.merchantId.eq(merchantId)).and(qCustomerLevelPrice.accountBookId.eq(accountBookId))).fetch().stream().collect(Collectors.toMap(c -> c.getCustomerLevelId(), b -> b));
    }

    public void updateById(Product product, Long merchantId, Long accountBookId) {
        if (product.getId() == null) {
            throw new ServiceException("商品ID不能为空");
        }
        if (product.getEnabled() == null) {
            throw new ServiceException("状态不能为空");
        }
        Product existing = productRepository.findById(product.getId())
                .orElseThrow(() -> new ServiceException("商品不存在"));

        existing.setEnabled(product.getEnabled());
        productRepository.save(existing);

    }

    /**
     * 品牌列表（去重，含商品数量）
     */
    public List<Map<String, Object>> brandList(Long merchantId, Long accountBookId) {
        List<Tuple> tuples = jqf.select(qProduct.brand, qProduct.id.count())
                .from(qProduct)
                .where(qProduct.merchantId.eq(merchantId)
                        .and(qProduct.accountBookId.eq(accountBookId))
                        .and(qProduct.brand.isNotNull())
                        .and(qProduct.brand.ne("")))
                .groupBy(qProduct.brand)
                .orderBy(qProduct.brand.asc())
                .fetch();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : tuples) {
            Map<String, Object> m = new HashMap<>();
            m.put("brand", t.get(qProduct.brand));
            m.put("count", t.get(qProduct.id.count()));
            result.add(m);
        }
        return result;
    }

    /**
     * 重命名品牌：更新所有引用该品牌的商品
     */
    @Transactional
    public void renameBrand(String oldBrand, String newBrand, Long merchantId, Long accountBookId) {
        if (StrUtil.isBlank(oldBrand)) {
            throw new ServiceException("原品牌名称不能为空");
        }
        if (StrUtil.isBlank(newBrand)) {
            throw new ServiceException("新品牌名称不能为空");
        }
        jqf.update(qProduct)
                .set(qProduct.brand, newBrand.trim())
                .where(qProduct.brand.eq(oldBrand.trim())
                        .and(qProduct.merchantId.eq(merchantId))
                        .and(qProduct.accountBookId.eq(accountBookId)))
                .execute();
    }

    /**
     * 删除品牌：清空所有引用该品牌的商品
     */
    @Transactional
    public void deleteBrand(String brand, Long merchantId, Long accountBookId) {
        if (StrUtil.isBlank(brand)) {
            throw new ServiceException("品牌名称不能为空");
        }
        jqf.update(qProduct)
                .set(qProduct.brand, (String) null)
                .where(qProduct.brand.eq(brand.trim())
                        .and(qProduct.merchantId.eq(merchantId))
                        .and(qProduct.accountBookId.eq(accountBookId)))
                .execute();
    }

    @Data
    public static class Query implements TenantAware {

        public final BooleanBuilder builder = new BooleanBuilder();

        private String path;

        private String filter;

        private String brand;

        private Long productCategoryId;

        private Boolean enabled;

        private Long id;

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qProduct.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qProduct.accountBookId, accountBookId);
        }

        public void setEnabled(Boolean enabled) {
            if (enabled != null) {
                builder.and(qProduct.enabled.eq(enabled));
            }
        }

        public BooleanBuilder builders() {

            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                builder.and(qProduct.name.contains(filter).or(qProduct.code.contains(filter)).or(qProduct.pinyin.contains(filter)));
            }
            if (StrUtil.isNotBlank(brand)) {
                builder.and(qProduct.brand.contains(brand.trim()));
            }
            if (enabled != null) {
                builder.and(qProduct.enabled.eq(enabled));
            }
            if (id != null) {
                builder.and(qProduct.id.eq(id));
            }
            if (productCategoryId != null) {
                builder.and(qProduct.productCategoryId.eq(productCategoryId));
            }
            return builder;
        }

    }
}
