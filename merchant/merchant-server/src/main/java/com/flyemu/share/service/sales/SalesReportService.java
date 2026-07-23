package com.flyemu.share.service.sales;

import com.flyemu.share.repository.basic.CustomerRepository;
import com.flyemu.share.repository.basic.ProductCategoryRepository;
import com.flyemu.share.repository.basic.ProductRepository;
import com.flyemu.share.repository.basic.UnitRepository;
import com.flyemu.share.repository.basic.WarehouseRepository;
import com.flyemu.share.repository.sales.SalesOutboundItemRepository;
import com.flyemu.share.repository.sales.SalesOutboundRepository;
import com.flyemu.share.repository.sales.SalesReturnItemRepository;
import com.flyemu.share.repository.sales.SalesReturnRepository;
import com.flyemu.share.common.TenantAware;
import com.flyemu.share.constant.SalesReportConstant;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.sales.SalesReportItemDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.sales.*;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesReportForm;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** 销售报表 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesReportService extends BaseService {

    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private final static QProduct qProduct = QProduct.product;

    private final SalesOutboundItemRepository salesOutboundItemRepository;
    private final SalesReturnItemRepository salesReturnItemRepository;

    private final SalesOutboundRepository salesOutboundRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final UnitRepository unitRepository;
    private final WarehouseRepository warehouseRepository;
    private final CustomerRepository customerRepository;

    public PageResults<SalesReportItemDto> item(Page page, SalesReportForm form) {

        Long accountBookId = form.getAccountBookId();
        Long merchantId = form.getMerchantId();

        PageResults<SalesReportItemDto> results = new PageResults<>(new ArrayList<>(), page, 0);
        String salesType = form.getSalesType();
        if (StrUtil.isBlank(salesType)) {
            return results;
        }
        //销售出库单查询条件
        Specification<SalesOutbound> salesOutboundSpecification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 添加各种条件
            // 新增审核状态条件
            predicates.add(cb.equal(root.get("orderStatus"), OrderStatus.已审核));

            List<Long> customerIds = form.getCustomerIds();
            if (!CollectionUtils.isEmpty(customerIds)) {
                predicates.add(root.get("customerId").in(customerIds));
            }
            if (form.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("outboundDate"), form.getStartDate()));
            }
            if (form.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("outboundDate"), form.getEndDate()));
            }
            if(StrUtil.isNotBlank(form.getFilter())){
                predicates.add(cb.like(root.get("orderNo"), "%" + form.getFilter() + "%"));
            }
            // 添加客户分类查询条件
            List<Long> customerCategoryIds = form.getCustomerCategoryIds();
            if (!CollectionUtils.isEmpty(customerCategoryIds)) {
                // 创建与产品表的关联
                Root<Customer> customerRoot = query.from(Customer.class);
                predicates.add(cb.equal(root.get("customerId"), customerRoot.get("id")));
                predicates.add(cb.in(customerRoot.get("customerCategoryId")).value(customerCategoryIds));
            }
            predicates.add( cb.equal(root.get("accountBookId"), accountBookId));
            predicates.add( cb.equal(root.get("merchantId"), merchantId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        //销售出库单列表
        List<SalesOutbound> salesOutboundList = salesOutboundRepository.findAll(salesOutboundSpecification);
        if (CollectionUtils.isEmpty(salesOutboundList)
                && !StrUtil.equals(salesType, SalesReportConstant.SALES_TYPE_RETURN)) {
            return results;
        }
        List<Product> productList = loadProducts(merchantId, accountBookId);
        List<ProductCategory> productCategoryList = productCategoryRepository.findAll();
        List<Unit> unitList = unitRepository.findAll();
        List<Warehouse> warehouseList = warehouseRepository.findAll();
        List<Customer> customerList = customerRepository.findAll();

        //销售出库单idList
        List<Long> salesOutboundIdList = salesOutboundList.stream().map(SalesOutbound::getId).toList();
        //根据销售出库单idList查询销售出库单商品详情
        List<SalesOutboundItem> outboundItemList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(salesOutboundIdList)) {
            Specification<SalesOutboundItem> salesOutboundItemSpecification = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                //查询销售出库单下面的商品
                predicates.add(root.get("salesOutboundId").in(salesOutboundIdList));
                List<Long> productIds = form.getProductIds();
                if (!CollectionUtils.isEmpty(productIds)){
                    predicates.add(root.get("productId").in(productIds));
                }
                List<Long> warehouseIds = form.getWarehouseIds();
                if (!CollectionUtils.isEmpty(warehouseIds)){
                    predicates.add(root.get("warehouseId").in(warehouseIds));
                }

                // 添加产品分类查询条件
                List<Long> productCategoryIds = form.getProductCategoryIds();
                if (!CollectionUtils.isEmpty(productCategoryIds)) {
                    // 创建与产品表的关联
                    Root<Product> productRoot = query.from(Product.class);
                    predicates.add(cb.equal(root.get("productId"), productRoot.get("id")));
                    predicates.add(cb.in(productRoot.get("productCategoryId")).value(productCategoryIds));
                }
                predicates.add( cb.equal(root.get("accountBookId"), accountBookId));
                predicates.add( cb.equal(root.get("merchantId"), merchantId));
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            //销售出库单商品详情list
            outboundItemList = salesOutboundItemRepository.findAll(salesOutboundItemSpecification);
        }
        if (StrUtil.equals(salesType, SalesReportConstant.SALES_TYPE_OUT)){
            List<SalesReportItemDto> outItemDTOList = getSalesReportOutItemDTOS(outboundItemList, productCategoryList, productList, unitList, warehouseList, salesOutboundList, customerList);
            return getSalesReportItemDtoPageResults(page, outItemDTOList);
        }

        List<SalesReturn> salesReturnList = findSalesReturns(form, accountBookId, merchantId);
        List<SalesReturnItem> returnItemList = findSalesReturnItems(form, salesReturnList, accountBookId, merchantId);

        if (StrUtil.equals(salesType, SalesReportConstant.SALES_TYPE_RETURN)) {
            List<SalesReportItemDto> returnItemDTOList = getSalesReportReturnItemDTOS(returnItemList, productCategoryList, productList, unitList, warehouseList, salesReturnList, customerList);
            return getSalesReportItemDtoPageResults(page, returnItemDTOList);
        }

        List<SalesReportItemDto> resultList = new ArrayList<>();
        if (StrUtil.equals(salesType, SalesReportConstant.SALES_TYPE_ALL)){
            List<SalesReportItemDto> outItemDTOList = getSalesReportOutItemDTOS(outboundItemList, productCategoryList, productList, unitList, warehouseList, salesOutboundList, customerList);
            List<SalesReportItemDto> returnItemDTOList = getSalesReportReturnItemDTOS(returnItemList, productCategoryList, productList, unitList, warehouseList, salesReturnList, customerList);
            resultList.addAll(outItemDTOList);
            resultList.addAll(returnItemDTOList);
            //降序排序
            resultList.sort(Comparator.comparing(SalesReportItemDto::getOrderDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        }
        return getSalesReportItemDtoPageResults(page, resultList);
    }

    private List<SalesReturn> findSalesReturns(SalesReportForm form, Long accountBookId, Long merchantId) {
        Specification<SalesReturn> returnOrderQuery = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("orderStatus"), OrderStatus.已审核));
            predicates.add(cb.equal(root.get("accountBookId"), accountBookId));
            predicates.add(cb.equal(root.get("merchantId"), merchantId));
            List<Long> customerIds = form.getCustomerIds();
            if (!CollectionUtils.isEmpty(customerIds)) {
                predicates.add(root.get("customerId").in(customerIds));
            }
            if (form.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("returnDate"), form.getStartDate()));
            }
            if (form.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("returnDate"), form.getEndDate()));
            }
            if (StrUtil.isNotBlank(form.getFilter())) {
                predicates.add(cb.like(root.get("orderNo"), "%" + form.getFilter() + "%"));
            }
            List<Long> customerCategoryIds = form.getCustomerCategoryIds();
            if (!CollectionUtils.isEmpty(customerCategoryIds)) {
                Root<Customer> customerRoot = query.from(Customer.class);
                predicates.add(cb.equal(root.get("customerId"), customerRoot.get("id")));
                predicates.add(cb.in(customerRoot.get("customerCategoryId")).value(customerCategoryIds));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return salesReturnRepository.findAll(returnOrderQuery);
    }

    private List<SalesReturnItem> findSalesReturnItems(SalesReportForm form, List<SalesReturn> salesReturnList,
                                                       Long accountBookId, Long merchantId) {
        if (CollectionUtils.isEmpty(salesReturnList)) {
            return new ArrayList<>();
        }
        List<Long> returnIds = salesReturnList.stream().map(SalesReturn::getId).toList();
        Specification<SalesReturnItem> returnOrderItemQuery = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("salesReturnId").in(returnIds));
            List<Long> productIds = form.getProductIds();
            if (!CollectionUtils.isEmpty(productIds)) {
                predicates.add(root.get("productId").in(productIds));
            }
            List<Long> warehouseIds = form.getWarehouseIds();
            if (!CollectionUtils.isEmpty(warehouseIds)) {
                predicates.add(root.get("warehouseId").in(warehouseIds));
            }
            List<Long> productCategoryIds = form.getProductCategoryIds();
            if (!CollectionUtils.isEmpty(productCategoryIds)) {
                Root<Product> productRoot = query.from(Product.class);
                predicates.add(cb.equal(root.get("productId"), productRoot.get("id")));
                predicates.add(cb.in(productRoot.get("productCategoryId")).value(productCategoryIds));
            }
            predicates.add(cb.equal(root.get("accountBookId"), accountBookId));
            predicates.add(cb.equal(root.get("merchantId"), merchantId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return salesReturnItemRepository.findAll(returnOrderItemQuery);
    }

    private List<SalesReportItemDto> getSalesReportOutItemDTOS(List<SalesOutboundItem> outboundItemList, List<ProductCategory> productCategoryList, List<Product> productList, List<Unit> unitList, List<Warehouse> warehouseList, List<SalesOutbound> salesOutboundList, List<Customer> customerList) {
        return outboundItemList.stream().map(item -> {
            SalesReportItemDto salesReportItemDTO = new SalesReportItemDto();
            BeanUtils.copyProperties(item, salesReportItemDTO);
            Long productId = item.getProductId();
            Optional<Product> productOptional = productList.stream().filter(product -> product.getId().equals(productId)).findFirst();
            productOptional.ifPresent(product -> {
                salesReportItemDTO.setProductName(product.getName());
                salesReportItemDTO.setProductCode(product.getCode());
                salesReportItemDTO.setSpecification(product.getSpecification());
                Long productCategoryId = product.getProductCategoryId();
                salesReportItemDTO.setProductCategoryId(productCategoryId);
                productCategoryList.stream().filter(productCategory -> productCategory.getId().equals(productCategoryId)).findFirst().ifPresent(productCategory -> {
                    salesReportItemDTO.setProductCategoryName(productCategory.getName());
                });
            });
            //单位信息
            Long baseUnitId = item.getBaseUnitId();
            unitList.stream().filter(unit -> unit.getId().equals(baseUnitId)).findFirst().ifPresent(unit -> {
                salesReportItemDTO.setUnitName(unit.getName());
            });

            Long warehouseId = item.getWarehouseId();
            warehouseList.stream().filter(warehouse -> warehouse.getId().equals(warehouseId)).findFirst().ifPresent(warehouse -> {
                salesReportItemDTO.setWarehouseName(warehouse.getName());
            });

            Long salesOutboundId = item.getSalesOutboundId();
            salesOutboundList.stream().filter(salesOutbound -> salesOutbound.getId().equals(salesOutboundId)).findFirst().ifPresent(salesOutbound -> {
                salesReportItemDTO.setOrderDate(salesOutbound.getOutboundDate());
                salesReportItemDTO.setOrderNo(salesOutbound.getOrderNo());
                //封装客户名称
                Long customerId = salesOutbound.getCustomerId();
                salesReportItemDTO.setCustomerId(customerId);
                customerList.stream().filter(customer -> customer.getId().equals(customerId)).findFirst().ifPresent(customer -> {
                    salesReportItemDTO.setCustomerName(customer.getName());
                    salesReportItemDTO.setCustomerCode(customer.getCode());
                    salesReportItemDTO.setCustomerCategoryId(customer.getCustomerCategoryId());
                });
            });
            salesReportItemDTO.setSalesType("out");
            return salesReportItemDTO;
        }).toList();
    }

    private List<SalesReportItemDto> getSalesReportReturnItemDTOS(List<SalesReturnItem> returnItemList, List<ProductCategory> productCategoryList, List<Product> productList, List<Unit> unitList, List<Warehouse> warehouseList, List<SalesReturn> salesReturnList, List<Customer> customerList) {
        return returnItemList.stream().map(item -> {
            SalesReportItemDto salesReportItemDTO = new SalesReportItemDto();
            BeanUtils.copyProperties(item, salesReportItemDTO);
            Long productId = item.getProductId();
            Optional<Product> productOptional = productList.stream().filter(product -> product.getId().equals(productId)).findFirst();
            productOptional.ifPresent(product -> {
                salesReportItemDTO.setProductName(product.getName());
                salesReportItemDTO.setProductCode(product.getCode());
                salesReportItemDTO.setSpecification(product.getSpecification());
                Long productCategoryId = product.getProductCategoryId();
                salesReportItemDTO.setProductCategoryId(productCategoryId);
                productCategoryList.stream().filter(productCategory -> productCategory.getId().equals(productCategoryId)).findFirst().ifPresent(productCategory -> {
                    salesReportItemDTO.setProductCategoryName(productCategory.getName());
                });
            });
            //单位信息
            Long baseUnitId = item.getBaseUnitId();
            unitList.stream().filter(unit -> unit.getId().equals(baseUnitId)).findFirst().ifPresent(unit -> {
                salesReportItemDTO.setUnitName(unit.getName());
            });

            Long warehouseId = item.getWarehouseId();
            warehouseList.stream().filter(warehouse -> warehouse.getId().equals(warehouseId)).findFirst().ifPresent(warehouse -> {
                salesReportItemDTO.setWarehouseName(warehouse.getName());
            });

            Long salesReturnId = item.getSalesReturnId();
            salesReturnList.stream().filter(salesReturn -> salesReturn.getId().equals(salesReturnId)).findFirst().ifPresent(salesReturn -> {
                salesReportItemDTO.setOrderDate(salesReturn.getReturnDate());
                salesReportItemDTO.setOrderNo(salesReturn.getOrderNo());
                //封装客户名称
                Long customerId = salesReturn.getCustomerId();
                salesReportItemDTO.setCustomerId(customerId);
                customerList.stream().filter(customer -> customer.getId().equals(customerId)).findFirst().ifPresent(customer -> {
                    salesReportItemDTO.setCustomerName(customer.getName());
                    salesReportItemDTO.setCustomerCode(customer.getCode());
                    salesReportItemDTO.setCustomerCategoryId(customer.getCustomerCategoryId());
                });
            });

            //退货取反
            BigDecimal subtotal = salesReportItemDTO.getSubtotal();
            BigDecimal quantity = salesReportItemDTO.getQuantity();
            BigDecimal costAmount = salesReportItemDTO.getCostAmount();
            if (subtotal != null) {
                salesReportItemDTO.setSubtotal(subtotal.negate());
            }
            if (quantity != null) {
                salesReportItemDTO.setQuantity(quantity.negate());
            }
            if (costAmount != null) {
                salesReportItemDTO.setCostAmount(costAmount.negate());
            }
            salesReportItemDTO.setSalesType("return");
            return salesReportItemDTO;
        }).toList();
    }

    private PageResults<SalesReportItemDto> getSalesReportItemDtoPageResults(Page page, List<SalesReportItemDto> resultList) {
        //返回分页数据
        int totalSize = resultList.size();
        int fromIndex = page.getOffset();
        if (fromIndex >= totalSize) {
            return new PageResults<>(new ArrayList<>(), page, totalSize);
        }
        int toIndex = Math.min(fromIndex + page.getOffsetEnd(), totalSize);
        List<SalesReportItemDto> pagedDTOList = resultList.subList(fromIndex, toIndex);
        return new PageResults<>(pagedDTOList, page, totalSize);
    }

    public PageResults<SalesReportItemDto> summary(Page page, SalesReportForm form) {

        Long accountBookId = form.getAccountBookId();
        Long merchantId = form.getMerchantId();
        PageResults<SalesReportItemDto> results = new PageResults<>(new ArrayList<>(), page, 0);
        String salesGroup = form.getSalesGroup();
        if (!isKnownSalesGroup(salesGroup)) {
            return results;
        }
        //销售出库单查询条件
        Specification<SalesOutbound> salesOutboundSpecification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 新增审核状态条件
            predicates.add(cb.equal(root.get("orderStatus"), OrderStatus.已审核));
            // 添加各种条件
            List<Long> customerIds = form.getCustomerIds();
            if (!CollectionUtils.isEmpty(customerIds)) {
                predicates.add(root.get("customerId").in(customerIds));
            }
            if (form.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("outboundDate"), form.getStartDate()));
            }
            if (form.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("outboundDate"), form.getEndDate()));
            }
            if(StrUtil.isNotBlank(form.getFilter())){
                predicates.add(cb.like(root.get("orderNo"), "%" + form.getFilter() + "%"));
            }
            // 添加客户分类查询条件
            List<Long> customerCategoryIds = form.getCustomerCategoryIds();
            if (!CollectionUtils.isEmpty(customerCategoryIds)) {
                // 创建与产品表的关联
                Root<Customer> customerRoot = query.from(Customer.class);
                predicates.add(cb.equal(root.get("customerId"), customerRoot.get("id")));
                predicates.add(cb.in(customerRoot.get("customerCategoryId")).value(customerCategoryIds));
            }
            predicates.add( cb.equal(root.get("accountBookId"), accountBookId));
            predicates.add( cb.equal(root.get("merchantId"), merchantId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        //销售出库单列表
        List<SalesOutbound> salesOutboundList = salesOutboundRepository.findAll(salesOutboundSpecification);
        List<Product> productList = loadProducts(merchantId, accountBookId);
        List<ProductCategory> productCategoryList = productCategoryRepository.findAll();
        List<Unit> unitList = unitRepository.findAll();
        List<Warehouse> warehouseList = warehouseRepository.findAll();
        List<Customer> customerList = customerRepository.findAll();

        //返回数据
        List<SalesReportItemDto> resultList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(salesOutboundList)) {
            //销售出库单idList
            List<Long> salesOutboundIdList = salesOutboundList.stream().map(SalesOutbound::getId).toList();
            //销售出库单商品查询条件
            Specification<SalesOutboundItem> salesOutboundItemSpecification = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                //查询销售出库单下面的商品
                predicates.add(root.get("salesOutboundId").in(salesOutboundIdList));
                List<Long> productIds = form.getProductIds();
                if (!CollectionUtils.isEmpty(productIds)){
                    predicates.add(root.get("productId").in(productIds));
                }
                List<Long> warehouseIds = form.getWarehouseIds();
                if (!CollectionUtils.isEmpty(warehouseIds)){
                    predicates.add(root.get("warehouseId").in(warehouseIds));
                }
                // 添加产品分类查询条件
                List<Long> productCategoryIds = form.getProductCategoryIds();
                if (!CollectionUtils.isEmpty(productCategoryIds)) {
                    // 创建与产品表的关联
                    Root<Product> productRoot = query.from(Product.class);
                    predicates.add(cb.equal(root.get("productId"), productRoot.get("id")));
                    predicates.add(cb.in(productRoot.get("productCategoryId")).value(productCategoryIds));
                }
                predicates.add( cb.equal(root.get("accountBookId"), accountBookId));
                predicates.add( cb.equal(root.get("merchantId"), merchantId));
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            //销售出库单商品列表
            List<SalesOutboundItem> outboundItemList = salesOutboundItemRepository.findAll(salesOutboundItemSpecification);
            //封装销售出库单商品列表
            List<SalesReportItemDto> outItemDTOList = getSalesReportOutItemDTOS(outboundItemList, productCategoryList, productList, unitList, warehouseList, salesOutboundList, customerList);
            resultList.addAll(outItemDTOList);
        }

        List<SalesReturn> salesReturnList = findSalesReturns(form, accountBookId, merchantId);
        List<SalesReturnItem> returnItemList = findSalesReturnItems(form, salesReturnList, accountBookId, merchantId);
        if (!CollectionUtils.isEmpty(returnItemList)) {
            List<SalesReportItemDto> returnItemDTOList = getSalesReportReturnItemDTOS(returnItemList, productCategoryList, productList, unitList, warehouseList, salesReturnList, customerList);
            resultList.addAll(returnItemDTOList);
        }
        if (CollectionUtils.isEmpty(resultList)) {
            return results;
        }
        //返回dtos
        List<SalesReportItemDto> dtos = new ArrayList<>();
        if (StrUtil.equals(SalesReportConstant.SALES_GROUP_PRODUCT, salesGroup)) {
            Map<Long, SalesReportItemDto> productSummary = resultList.stream()
                .filter(item -> item.getProductId() != null)
                .collect(Collectors.groupingBy(
                    SalesReportItemDto::getProductId,
                    Collectors.collectingAndThen(Collectors.toList(),
                        items -> {
                            SalesReportItemDto dto = new SalesReportItemDto();
                            SalesReportItemDto firstItem = items.get(0);
                            dto.setProductId(firstItem.getProductId());
                            dto.setProductName(firstItem.getProductName());
                            dto.setProductCode(firstItem.getProductCode());
                            dto.setProductCategoryId(firstItem.getProductCategoryId());
                            dto.setProductCategoryName(firstItem.getProductCategoryName());
                            dto.setUnitName(firstItem.getUnitName());
                            aggregateQtyAndAmount(dto, items);
                            return dto;
                        }
                    )
                ));
            dtos.addAll(productSummary.values());
        } else if (StrUtil.equals(SalesReportConstant.SALES_GROUP_CUSTOMER, salesGroup)) {
            Map<Long, SalesReportItemDto> customerSummary = resultList.stream()
                    .filter(item -> item.getCustomerId() != null)
                    .collect(Collectors.groupingBy(
                            SalesReportItemDto::getCustomerId,
                            Collectors.collectingAndThen(Collectors.toList(),
                                    items -> {
                                        SalesReportItemDto dto = new SalesReportItemDto();
                                        SalesReportItemDto firstItem = items.get(0);
                                        dto.setCustomerId(firstItem.getCustomerId());
                                        dto.setCustomerName(firstItem.getCustomerName());
                                        dto.setCustomerCode(firstItem.getCustomerCode());
                                        dto.setCustomerCategoryId(firstItem.getCustomerCategoryId());
                                        aggregateQtyAndAmount(dto, items);
                                        return dto;
                                    }
                            )
                    ));
            dtos.addAll(customerSummary.values());
        } else if (StrUtil.equals(SalesReportConstant.SALES_GROUP_PRODUCT_WAREHOUSE, salesGroup)) {
            Map<String, SalesReportItemDto> productWarehouseSummary = resultList.stream()
                .filter(item -> item.getProductId() != null && item.getWarehouseId() != null)
                .collect(Collectors.groupingBy(item -> item.getProductId() + "-" + item.getWarehouseId(),
                Collectors.collectingAndThen(Collectors.toList(),
                    items -> {
                        SalesReportItemDto dto = new SalesReportItemDto();
                        SalesReportItemDto firstItem = items.get(0);
                        dto.setProductId(firstItem.getProductId());
                        dto.setProductName(firstItem.getProductName());
                        dto.setProductCode(firstItem.getProductCode());
                        dto.setProductCategoryId(firstItem.getProductCategoryId());
                        dto.setProductCategoryName(firstItem.getProductCategoryName());
                        dto.setUnitName(firstItem.getUnitName());
                        dto.setSpecification(firstItem.getSpecification());
                        dto.setWarehouseId(firstItem.getWarehouseId());
                        dto.setWarehouseName(firstItem.getWarehouseName());
                        aggregateQtyAndAmount(dto, items);
                        return dto;
                    }
                )
            ));
            dtos.addAll(productWarehouseSummary.values());
        } else if (StrUtil.equals(SalesReportConstant.SALES_GROUP_CUSTOMER_PRODUCT, salesGroup)) {
            Map<String, SalesReportItemDto> productCustomerSummary = resultList.stream()
                    .filter(item -> item.getProductId() != null && item.getCustomerId() != null)
                    .collect(Collectors.groupingBy(item -> item.getProductId() + "-" + item.getCustomerId(),
                    Collectors.collectingAndThen(Collectors.toList(),
                            items -> {
                                SalesReportItemDto dto = new SalesReportItemDto();
                                SalesReportItemDto firstItem = items.get(0);
                                dto.setProductId(firstItem.getProductId());
                                dto.setProductName(firstItem.getProductName());
                                dto.setProductCode(firstItem.getProductCode());
                                dto.setProductCategoryId(firstItem.getProductCategoryId());
                                dto.setProductCategoryName(firstItem.getProductCategoryName());
                                dto.setUnitName(firstItem.getUnitName());
                                dto.setSpecification(firstItem.getSpecification());
                                dto.setCustomerId(firstItem.getCustomerId());
                                dto.setCustomerName(firstItem.getCustomerName());
                                dto.setCustomerCode(firstItem.getCustomerCode());
                                dto.setCustomerCategoryId(firstItem.getCustomerCategoryId());
                                aggregateQtyAndAmount(dto, items);
                                return dto;
                            }
                    )
            ));
            dtos.addAll(productCustomerSummary.values());
        } else if (StrUtil.equals(SalesReportConstant.SALES_GROUP_CUSTOMER_PRODUCT_WAREHOUSE, salesGroup)) {
            Map<String, SalesReportItemDto> productCustomerWarehouseSummary = resultList.stream()
                    .filter(item -> item.getProductId() != null && item.getCustomerId() != null && item.getWarehouseId() != null)
                    .collect(Collectors.groupingBy(
                    item -> item.getProductId() + "-" + item.getCustomerId() + "-" + item.getWarehouseId(),
                    Collectors.collectingAndThen(Collectors.toList(),
                            items -> {
                                SalesReportItemDto dto = new SalesReportItemDto();
                                SalesReportItemDto firstItem = items.get(0);
                                dto.setProductId(firstItem.getProductId());
                                dto.setProductName(firstItem.getProductName());
                                dto.setProductCode(firstItem.getProductCode());
                                dto.setProductCategoryId(firstItem.getProductCategoryId());
                                dto.setProductCategoryName(firstItem.getProductCategoryName());
                                dto.setUnitName(firstItem.getUnitName());
                                dto.setSpecification(firstItem.getSpecification());
                                dto.setCustomerId(firstItem.getCustomerId());
                                dto.setCustomerName(firstItem.getCustomerName());
                                dto.setCustomerCode(firstItem.getCustomerCode());
                                dto.setCustomerCategoryId(firstItem.getCustomerCategoryId());
                                dto.setWarehouseId(firstItem.getWarehouseId());
                                dto.setWarehouseName(firstItem.getWarehouseName());
                                aggregateQtyAndAmount(dto, items);
                                return dto;
                            }
                    )
            ));
            dtos.addAll(productCustomerWarehouseSummary.values());
        }
        //移除数量为0的数据
        dtos.removeIf(item -> item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) == 0);
        //返回分页数据
        return getSalesReportItemDtoPageResults(page, dtos);
    }

    /**
     * 销售利润表（按产品汇总，成本优先取出库审核落库成本，缺省回退预计进货价）
     */
    public PageResults<SalesReportItemDto> profit(Page page, SalesReportForm form) {
        form.setSalesGroup(SalesReportConstant.SALES_GROUP_PRODUCT);
        PageResults<SalesReportItemDto> summaryResult = summary(page, form);
        Map<Long, Product> productMap = loadProducts(form.getMerchantId(), form.getAccountBookId()).stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        Collection<SalesReportItemDto> profitRows = summaryResult.getResults();
        if (profitRows != null) {
            for (SalesReportItemDto dto : profitRows) {
                fillProfitFields(dto, dto.getProductId() == null ? null : productMap.get(dto.getProductId()));
            }
        }
        return summaryResult;
    }

    /**
     * 销售排行表（按产品或客户）
     */
    public PageResults<SalesReportItemDto> ranking(Page page, SalesReportForm form) {
        String rankingType = StrUtil.blankToDefault(form.getRankingType(), SalesReportConstant.SALES_GROUP_PRODUCT);
        if (StrUtil.equals(rankingType, SalesReportConstant.SALES_GROUP_CUSTOMER)
                || StrUtil.equalsIgnoreCase(rankingType, "CUSTOMER")) {
            form.setSalesGroup(SalesReportConstant.SALES_GROUP_CUSTOMER);
        } else {
            form.setSalesGroup(SalesReportConstant.SALES_GROUP_PRODUCT);
        }
        // 先取全量再排行分页
        Page fullPage = new Page();
        fullPage.setPage(1);
        fullPage.setPageSize(100000);
        PageResults<SalesReportItemDto> summaryResult = summary(fullPage, form);
        List<SalesReportItemDto> list = new ArrayList<>(
                summaryResult.getResults() == null ? Collections.emptyList() : summaryResult.getResults());
        list.sort(Comparator.comparing(
                (SalesReportItemDto item) -> item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal()
        ).reversed());
        int rank = 1;
        for (SalesReportItemDto dto : list) {
            dto.setRankNo(rank++);
        }
        return getSalesReportItemDtoPageResults(page, list);
    }

    private void fillProfitFields(SalesReportItemDto dto, Product product) {
        BigDecimal qty = dto.getQuantity() == null ? BigDecimal.ZERO : dto.getQuantity();
        BigDecimal salesAmount = dto.getSubtotal() == null ? BigDecimal.ZERO : dto.getSubtotal();
        BigDecimal costAmount = dto.getCostAmount();
        BigDecimal costPrice = dto.getCostPrice();
        // 历史单据未落成本时，回退产品档案采购价
        if (costAmount == null || costAmount.compareTo(BigDecimal.ZERO) == 0) {
            costPrice = product == null || product.getPurchasePrice() == null
                    ? BigDecimal.ZERO : product.getPurchasePrice();
            costAmount = costPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);
        } else if (costPrice == null && qty.compareTo(BigDecimal.ZERO) != 0) {
            costPrice = costAmount.divide(qty, 2, RoundingMode.HALF_UP);
        } else if (costPrice == null) {
            costPrice = BigDecimal.ZERO;
        }
        BigDecimal profitAmount = salesAmount.subtract(costAmount).setScale(2, RoundingMode.HALF_UP);
        dto.setCostPrice(costPrice);
        dto.setCostAmount(costAmount);
        dto.setProfitAmount(profitAmount);
        if (salesAmount.compareTo(BigDecimal.ZERO) == 0) {
            dto.setProfitRate(BigDecimal.ZERO);
        } else {
            dto.setProfitRate(profitAmount.multiply(new BigDecimal("100"))
                    .divide(salesAmount, 2, RoundingMode.HALF_UP));
        }
    }

    /** Product has merchantId/accountBookId — prefer scoped load over findAll. */
    private List<Product> loadProducts(Long merchantId, Long accountBookId) {
        return productRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (merchantId != null) {
                predicates.add(cb.equal(root.get("merchantId"), merchantId));
            }
            if (accountBookId != null) {
                predicates.add(cb.equal(root.get("accountBookId"), accountBookId));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }

    private boolean isKnownSalesGroup(String salesGroup) {
        return StrUtil.equals(salesGroup, SalesReportConstant.SALES_GROUP_PRODUCT)
                || StrUtil.equals(salesGroup, SalesReportConstant.SALES_GROUP_CUSTOMER)
                || StrUtil.equals(salesGroup, SalesReportConstant.SALES_GROUP_PRODUCT_WAREHOUSE)
                || StrUtil.equals(salesGroup, SalesReportConstant.SALES_GROUP_CUSTOMER_PRODUCT)
                || StrUtil.equals(salesGroup, SalesReportConstant.SALES_GROUP_CUSTOMER_PRODUCT_WAREHOUSE);
    }

    private void aggregateQtyAndAmount(SalesReportItemDto dto, List<SalesReportItemDto> items) {
        dto.setQuantity(items.stream()
                .map(item -> item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setSubtotal(items.stream()
                .map(item -> item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setCostAmount(items.stream()
                .map(item -> item.getCostAmount() != null ? item.getCostAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        if (dto.getQuantity() != null && dto.getQuantity().compareTo(BigDecimal.ZERO) != 0) {
            dto.setUnitPrice(dto.getSubtotal().divide(dto.getQuantity(), 2, RoundingMode.HALF_UP));
            dto.setCostPrice(dto.getCostAmount().divide(dto.getQuantity(), 2, RoundingMode.HALF_UP));
        } else {
            dto.setUnitPrice(BigDecimal.ZERO);
            dto.setCostPrice(BigDecimal.ZERO);
        }
    }

    @Data
    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();
        StringBuilder whereClause = new StringBuilder();
        @Getter
        Map<String, Object> params = new HashMap<>();

        private String salesType;

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSalesOutbound.merchantId.eq(merchantId));
                whereClause.append(" AND so.merchant_id = :merchantId");
                params.put("merchantId", merchantId);
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSalesOutbound.accountBookId.eq(accountBookId));
                whereClause.append(" AND so.account_book_id = :accountBookId");
                params.put("accountBookId", accountBookId);

            }
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotBlank(filter)) {
                builder.and(qSalesOutbound.orderNo.like("%" + filter + "%"));
                //whereClause.append(" AND so.order_no LIKE '%").append(":filter").append("%'");
                whereClause.append(" AND so.order_no LIKE :filter");
                params.put("filter", filter);

            }
        }

        public void setState(String state) {
            if (StrUtil.isNotBlank(state)) {
                builder.and(qSalesOutbound.orderStatus.eq(OrderStatus.valueOf(state)));
                whereClause.append(" AND so.order_status = :state");
                params.put("state", state);

            }
        }

        public void setStart(String start) {
            if (StrUtil.isNotBlank(start)) {
                whereClause.append(" AND so.outbound_date >= :start");
                params.put("start", start);
            }
        }

        public void setEnd(String end) {
            if (StrUtil.isNotBlank(end)) {
                whereClause.append(" AND so.outbound_date <= :end");
                params.put("end", end);
            }
        }

        public void setCustomerId(Long customerId) {
            if (customerId != null) {
                builder.and(qSalesOutbound.customerId.eq(customerId));
                whereClause.append(" AND so.customer_id = :customerId");
                params.put("customerId", customerId);
            }
        }

        //查询未退货订单
        public void setQueryUnReturnOrder(Integer queryUnReturnOrder) {
            if (queryUnReturnOrder == 1) {
                builder.and(qSalesOutbound.returnOrderId.isNull());
            }
        }

        public String getWhereClause() {
            return whereClause.toString();
        }

    }
}
