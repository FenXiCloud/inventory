package com.flyemu.share.service.sales;

import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.constant.SalesReportConstant;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.SalesOrderDTO;
import com.flyemu.share.dto.SalesOrderItemDTO;
import com.flyemu.share.dto.SalesReportItemDTO;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.sales.*;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.form.SalesReportForm;
import com.flyemu.share.repository.*;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.sagacity.sqltoy.dao.SqlToyLazyDao;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @功能描述: 销售报表
 * @作者: wl
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesReportService extends AbsService {

    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private final static QProduct qProduct = QProduct.product;

    private final SalesOutboundItemRepository salesOutboundItemRepository;
    private final SalesReturnItemRepository salesReturnItemRepository;

    private final SalesOutboundRepository salesOutboundRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final ProductRepository productRepository;
    private final UnitRepository unitRepository;
    private final WarehouseRepository warehouseRepository;
    private final CustomerRepository customerRepository;


    public PageResults<SalesReportItemDTO> salesItem(Page page, SalesReportForm form) {

        PageResults<SalesReportItemDTO> results = new PageResults<>(new ArrayList<>(),page,0);
        String salesType = form.getSalesType();
        if (StringUtils.isBlank(salesType)) {
            return results;
        }
        //销售出库单查询条件
        Specification<SalesOutbound> salesOutboundSpecification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 添加各种条件
            // 新增审核状态条件
            predicates.add(cb.equal(root.get("orderStatus"), OrderStatus.已审核));
            if (form.getCustomerId() != null) {
                predicates.add(cb.equal(root.get("customerId"), form.getCustomerId()));
            }
            if (form.getStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("outboundDate"), form.getStart()));
            }
            if (form.getEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("outboundDate"), form.getEnd()));
            }
            if(StringUtils.isNotBlank(form.getFilter())){
                predicates.add(cb.like(root.get("orderNo"), "%" + form.getFilter() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        //销售出库单列表
        List<SalesOutbound> salesOutboundList = salesOutboundRepository.findAll(salesOutboundSpecification);
        if (CollectionUtils.isEmpty(salesOutboundList)) {
            return results;
        }
        List<Product> productList = productRepository.findAll();
        List<Unit> unitList = unitRepository.findAll();
        List<Warehouse> warehouseList = warehouseRepository.findAll();
        List<Customer> customerList = customerRepository.findAll();


        //销售出库单idList
        List<Long> salesOutboundIdList = salesOutboundList.stream().map(SalesOutbound::getId).toList();
        //根据销售出库单idList查询销售出库单商品详情
        Specification<SalesOutboundItem> salesOutboundItemSpecification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 创建与产品表的关联
            Root<Product> productRoot = query.from(Product.class);
            predicates.add(cb.equal(root.get("productId"), productRoot.get("id")));
            
            //查询销售出库单下面的商品
            if (!salesOutboundIdList.isEmpty()){
                predicates.add(root.get("salesOutboundId").in(salesOutboundIdList));
            }
            if (form.getProductId() != null){
                predicates.add(cb.equal(root.get("productId"), form.getProductId()));
            }
            if (form.getWarehouseId() != null){
                predicates.add(cb.equal(root.get("warehouseId"), form.getWarehouseId()));
            }
            // 添加产品分类查询条件
            if (form.getProductCategoryId() != null) {
                predicates.add(cb.equal(productRoot.get("productCategoryId"), form.getProductCategoryId()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        //销售出库单商品详情list
        List<SalesOutboundItem> outboundItemList = salesOutboundItemRepository.findAll(salesOutboundItemSpecification);
        if (StringUtils.equals(salesType, SalesReportConstant.SALES_TYPE_OUT)){
            List<SalesReportItemDTO> outItemDTOList = getSalesReportOutItemDTOS(outboundItemList, productList, unitList, warehouseList, salesOutboundList, customerList);
            return getSalesReportItemDTOPageResults(page, outItemDTOList);
        }

        //销售退货单idList
        List<Long> salesReturnIdList = salesOutboundList.stream()
                .map(SalesOutbound::getReturnOrderId)
                .filter(Objects::nonNull)
                .toList();
        List<SalesReturn> salesReturnList = new ArrayList<>();
        List<SalesReturnItem> returnItemList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(salesReturnIdList)) {
            //销售退货单列表查询条件
            Specification<SalesReturn> returnOrderQuery = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(root.get("id").in(salesReturnIdList));
                predicates.add(cb.equal(root.get("orderStatus"), OrderStatus.已审核));
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            //销售退货单列表
            salesReturnList = salesReturnRepository.findAll(returnOrderQuery);
            //已审核的退货单id
            List<Long> returnIds = salesReturnList.stream().map(SalesReturn::getId).toList();
            //销售退货单商品详情查询条件
            Specification<SalesReturnItem> returnOrderItemQuery = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(root.get("salesReturnId").in(returnIds));
                if (form.getProductId() != null){
                    predicates.add(cb.equal(root.get("productId"), form.getProductId()));
                }
                if (form.getWarehouseId() != null){
                    predicates.add(cb.equal(root.get("warehouseId"), form.getWarehouseId()));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            //销售退货单商品详情
            returnItemList = salesReturnItemRepository.findAll(returnOrderItemQuery);
        }


        List<SalesReportItemDTO> resultList = new ArrayList<>();
        if (StringUtils.equals(salesType, SalesReportConstant.SALES_TYPE_RETURN)) {
            List<SalesReportItemDTO> returnItemDTOList = getSalesReportReturnItemDTOS(returnItemList, productList, unitList, warehouseList, salesReturnList, customerList);
            return getSalesReportItemDTOPageResults(page, returnItemDTOList);
        }

        if (StringUtils.equals(salesType, SalesReportConstant.SALES_TYPE_ALL)){
            List<SalesReportItemDTO> outItemDTOList = getSalesReportOutItemDTOS(outboundItemList, productList, unitList, warehouseList, salesOutboundList, customerList);
            List<SalesReportItemDTO> returnItemDTOList = getSalesReportReturnItemDTOS(returnItemList, productList, unitList, warehouseList, salesReturnList, customerList);
            resultList.addAll(outItemDTOList);
            resultList.addAll(returnItemDTOList);
            //降序排序
            resultList.sort(Comparator.comparing(SalesReportItemDTO::getOrderDate).reversed());
        }
        return getSalesReportItemDTOPageResults(page, resultList);
    }

    private List<SalesReportItemDTO> getSalesReportOutItemDTOS(List<SalesOutboundItem> outboundItemList, List<Product> productList, List<Unit> unitList, List<Warehouse> warehouseList, List<SalesOutbound> salesOutboundList, List<Customer> customerList) {
        List<SalesReportItemDTO> outItemDTOList = outboundItemList.stream().map(item -> {
            SalesReportItemDTO salesReportItemDTO = new SalesReportItemDTO();
            BeanUtils.copyProperties(item, salesReportItemDTO);
            Long productId = item.getProductId();
            Optional<Product> productOptional = productList.stream().filter(product -> product.getId().equals(productId)).findFirst();
            productOptional.ifPresent(product -> {
                salesReportItemDTO.setProductName(product.getName());
                salesReportItemDTO.setProductCode(product.getCode());
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
                });
            });
            salesReportItemDTO.setSalesType("out");
            return salesReportItemDTO;
        }).toList();
        return outItemDTOList;
    }

    private List<SalesReportItemDTO> getSalesReportReturnItemDTOS(List<SalesReturnItem> returnItemList, List<Product> productList, List<Unit> unitList, List<Warehouse> warehouseList, List<SalesReturn> salesReturnList, List<Customer> customerList) {
        List<SalesReportItemDTO> returnItemDTOList = returnItemList.stream().map(item -> {
            SalesReportItemDTO salesReportItemDTO = new SalesReportItemDTO();
            BeanUtils.copyProperties(item, salesReportItemDTO);
            Long productId = item.getProductId();
            Optional<Product> productOptional = productList.stream().filter(product -> product.getId().equals(productId)).findFirst();
            productOptional.ifPresent(product -> {
                salesReportItemDTO.setProductName(product.getName());
                salesReportItemDTO.setProductCode(product.getCode());
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
                });
            });

            //退货取反
            BigDecimal subtotal = salesReportItemDTO.getSubtotal();
            Double quantity = salesReportItemDTO.getQuantity();
            if (subtotal != null) {
                salesReportItemDTO.setSubtotal(subtotal.negate());
            }
            if (quantity != null) {
                salesReportItemDTO.setQuantity(-quantity);
            }
            salesReportItemDTO.setSalesType("return");
            return salesReportItemDTO;
        }).toList();
        return returnItemDTOList;
    }

    private PageResults<SalesReportItemDTO> getSalesReportItemDTOPageResults(Page page, List<SalesReportItemDTO> resultList) {
        //返回分页数据
        int totalSize = resultList.size();
        int fromIndex = page.getOffset();
        int toIndex = Math.min(fromIndex + page.getOffsetEnd(), resultList.size());
        List<SalesReportItemDTO> pagedDTOList = resultList.subList(fromIndex, toIndex);
        return new PageResults<>(pagedDTOList, page, totalSize);
    }

    public PageResults<SalesReportItemDTO> salesSummary(Page page, SalesReportForm form) {

        PageResults<SalesReportItemDTO> results = new PageResults<>(new ArrayList<>(),page,0);
        //销售出库单查询条件
        Specification<SalesOutbound> salesOutboundSpecification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 新增审核状态条件
            predicates.add(cb.equal(root.get("orderStatus"), OrderStatus.已审核));
            // 添加各种条件
            if (form.getCustomerId() != null) {
                predicates.add(cb.equal(root.get("customerId"), form.getCustomerId()));
            }
            if (form.getStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("outboundDate"), form.getStart()));
            }
            if (form.getEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("outboundDate"), form.getEnd()));
            }
            if(StringUtils.isNotBlank(form.getFilter())){
                predicates.add(cb.like(root.get("orderNo"), "%" + form.getFilter() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        //销售出库单列表
        List<SalesOutbound> salesOutboundList = salesOutboundRepository.findAll(salesOutboundSpecification);
        if (CollectionUtils.isEmpty(salesOutboundList)) {
            return results;
        }
        List<Product> productList = productRepository.findAll();
        List<Unit> unitList = unitRepository.findAll();
        List<Warehouse> warehouseList = warehouseRepository.findAll();
        List<Customer> customerList = customerRepository.findAll();

        //销售出库单idList
        List<Long> salesOutboundIdList = salesOutboundList.stream().map(SalesOutbound::getId).toList();
        //销售出库单商品查询条件
        Specification<SalesOutboundItem> salesOutboundItemSpecification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            //查询销售出库单下面的商品
            if (!salesOutboundIdList.isEmpty()){
                predicates.add(root.get("salesOutboundId").in(salesOutboundIdList));
            }
            if (form.getProductId() != null){
                predicates.add(cb.equal(root.get("productId"), form.getProductId()));
            }
            if (form.getWarehouseId() != null){
                predicates.add(cb.equal(root.get("warehouseId"), form.getWarehouseId()));
            }
            // 添加产品分类查询条件
            if (form.getProductCategoryId() != null) {
                // 创建与产品表的关联
                Root<Product> productRoot = query.from(Product.class);
                predicates.add(cb.equal(root.get("productId"), productRoot.get("id")));
                predicates.add(cb.equal(productRoot.get("productCategoryId"), form.getProductCategoryId()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        //销售出库单商品列表
        List<SalesOutboundItem> outboundItemList = salesOutboundItemRepository.findAll(salesOutboundItemSpecification);
        //返回数据
        List<SalesReportItemDTO> resultList = new ArrayList<>();
        //封装销售出库单商品列表
        List<SalesReportItemDTO> outItemDTOList = getSalesReportOutItemDTOS(outboundItemList, productList, unitList, warehouseList, salesOutboundList, customerList);
        resultList.addAll(outItemDTOList);

        //销售退货单idList
        List<Long> salesReturnIdList = salesOutboundList.stream()
                .map(SalesOutbound::getReturnOrderId)
                .filter(Objects::nonNull)
                .toList();
        if (!CollectionUtils.isEmpty(salesReturnIdList)) {
            //销售退货单列表查询条件
            Specification<SalesReturn> returnOrderQuery = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                // 新增审核状态条件
                predicates.add(cb.equal(root.get("orderStatus"), OrderStatus.已审核));
                predicates.add(root.get("id").in(salesReturnIdList));
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            //销售退货单列表
            List<SalesReturn> salesReturnList = salesReturnRepository.findAll(returnOrderQuery);
            //已审核的退货单id
            List<Long> returnIds = salesReturnList.stream().map(SalesReturn::getId).toList();
            //销售退货单商品查询条件
            Specification<SalesReturnItem> returnSpec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(root.get("salesReturnId").in(returnIds));
                if (form.getProductId() != null){
                    predicates.add(cb.equal(root.get("productId"), form.getProductId()));
                }
                if (form.getWarehouseId() != null){
                    predicates.add(cb.equal(root.get("warehouseId"), form.getWarehouseId()));
                }
                // 添加产品分类查询条件
                if (form.getProductCategoryId() != null) {
                    // 通过 join 关联 Product 表
                    Root<Product> productRoot = query.from(Product.class);
                    predicates.add(cb.equal(root.get("productId"), productRoot.get("id")));
                    predicates.add(cb.equal(productRoot.get("productCategoryId"), form.getProductCategoryId()));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            //销售退货单商品列表
            List<SalesReturnItem> returnItemList = salesReturnItemRepository.findAll(returnSpec);

            List<SalesReportItemDTO> returnItemDTOList = getSalesReportReturnItemDTOS(returnItemList, productList, unitList, warehouseList, salesReturnList, customerList);
            resultList.addAll(returnItemDTOList);
        }
        //返回dtos
        List<SalesReportItemDTO> dtos = new ArrayList<>();
        String salesGroup = form.getSalesGroup();
        if (StringUtils.equals(SalesReportConstant.SALES_GROUP_PRODUCT, salesGroup)) {
            Map<Long, SalesReportItemDTO> productSummary = resultList.stream()
                .collect(Collectors.groupingBy(
                    SalesReportItemDTO::getProductId,
                    Collectors.collectingAndThen(Collectors.toList(),
                        items -> {
                            SalesReportItemDTO dto = new SalesReportItemDTO();
                            SalesReportItemDTO firstItem = items.get(0);
                            dto.setProductId(firstItem.getProductId());
                            dto.setProductName(firstItem.getProductName());
                            dto.setProductCode(firstItem.getProductCode());
                            dto.setUnitName(firstItem.getUnitName());

                            dto.setQuantity(items.stream()
                                .mapToDouble(item -> item.getQuantity() != null ? item.getQuantity() : 0.0)
                                .sum());
                            dto.setSubtotal(items.stream()
                                .map(item -> item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                            //单价计算
                            if (dto.getQuantity() != 0) {
                                dto.setUnitPrice(dto.getSubtotal().divide(BigDecimal.valueOf(dto.getQuantity()), 2, BigDecimal.ROUND_HALF_UP));
                            } else {
                                dto.setUnitPrice(BigDecimal.ZERO);
                            }
                            return dto;
                        }
                    )
                ));
            dtos.addAll(productSummary.values());
        } else if (StringUtils.equals(SalesReportConstant.SALES_GROUP_PRODUCT_WAREHOUSE,salesGroup)){
            Map<String, SalesReportItemDTO> productWarehouseSummary = resultList.stream().collect(Collectors.groupingBy(item -> item.getProductId() + "-" + item.getWarehouseId(),
                Collectors.collectingAndThen(Collectors.toList(),
                    items -> {
                        SalesReportItemDTO dto = new SalesReportItemDTO();
                        SalesReportItemDTO firstItem = items.get(0);
                        dto.setProductId(firstItem.getProductId());
                        dto.setProductName(firstItem.getProductName());
                        dto.setProductCode(firstItem.getProductCode());
                        dto.setUnitName(firstItem.getUnitName());
                        dto.setWarehouseName(firstItem.getWarehouseName());

                        dto.setQuantity(items.stream()
                                .mapToDouble(item -> item.getQuantity() != null ? item.getQuantity() : 0.0)
                                .sum());
                        dto.setSubtotal(items.stream()
                                .map(item -> item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                        //单价计算
                        if (dto.getQuantity() != 0) {
                            dto.setUnitPrice(dto.getSubtotal().divide(BigDecimal.valueOf(dto.getQuantity()), 2, BigDecimal.ROUND_HALF_UP));
                        } else {
                            dto.setUnitPrice(BigDecimal.ZERO);
                        }
                        return dto;
                    }
                )
            ));
            dtos.addAll(productWarehouseSummary.values());
        }else if (StringUtils.equals(SalesReportConstant.SALES_GROUP_CUSTOMER_PRODUCT, salesGroup)) {
            Map<String, SalesReportItemDTO> productCustomerSummary = resultList.stream().collect(Collectors.groupingBy(item -> item.getProductId() + "-" + item.getCustomerId(),
                    Collectors.collectingAndThen(Collectors.toList(),
                            items -> {
                                SalesReportItemDTO dto = new SalesReportItemDTO();
                                SalesReportItemDTO firstItem = items.get(0);
                                dto.setProductId(firstItem.getProductId());
                                dto.setProductName(firstItem.getProductName());
                                dto.setProductCode(firstItem.getProductCode());
                                dto.setUnitName(firstItem.getUnitName());
                                dto.setCustomerId(firstItem.getCustomerId());
                                dto.setCustomerName(firstItem.getCustomerName());
                                dto.setCustomerCode(firstItem.getCustomerCode());

                                dto.setQuantity(items.stream()
                                        .mapToDouble(item -> item.getQuantity() != null ? item.getQuantity() : 0.0)
                                        .sum());
                                dto.setSubtotal(items.stream()
                                        .map(item -> item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                                //单价计算
                                if (dto.getQuantity() != 0) {
                                    dto.setUnitPrice(dto.getSubtotal().divide(BigDecimal.valueOf(dto.getQuantity()), 2, BigDecimal.ROUND_HALF_UP));
                                } else {
                                    dto.setUnitPrice(BigDecimal.ZERO);
                                }
                                return dto;
                            }
                    )
            ));
            dtos.addAll(productCustomerSummary.values());
        }else if (StringUtils.equals(SalesReportConstant.SALES_GROUP_CUSTOMER_PRODUCT_WAREHOUSE, salesGroup)) {
            Map<String, SalesReportItemDTO> productCustomerWarehouseSummary = resultList.stream().collect(
                    Collectors.groupingBy(item -> item.getProductId() + "-" + item.getCustomerId() + "-"+item.getWarehouseId(),
                    Collectors.collectingAndThen(Collectors.toList(),
                            items -> {
                                SalesReportItemDTO dto = new SalesReportItemDTO();
                                SalesReportItemDTO firstItem = items.get(0);
                                dto.setProductId(firstItem.getProductId());
                                dto.setProductName(firstItem.getProductName());
                                dto.setProductCode(firstItem.getProductCode());
                                dto.setUnitName(firstItem.getUnitName());
                                //客户信息
                                dto.setCustomerId(firstItem.getCustomerId());
                                dto.setCustomerName(firstItem.getCustomerName());
                                dto.setCustomerCode(firstItem.getCustomerCode());
                                //仓库信息
                                dto.setWarehouseId(firstItem.getWarehouseId());
                                dto.setWarehouseName(firstItem.getWarehouseName());

                                dto.setQuantity(items.stream()
                                        .mapToDouble(item -> item.getQuantity() != null ? item.getQuantity() : 0.0)
                                        .sum());
                                dto.setSubtotal(items.stream()
                                        .map(item -> item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                                //单价计算
                                if (dto.getQuantity() != 0) {
                                    dto.setUnitPrice(dto.getSubtotal().divide(BigDecimal.valueOf(dto.getQuantity()), 2, BigDecimal.ROUND_HALF_UP));
                                } else {
                                    dto.setUnitPrice(BigDecimal.ZERO);
                                }
                                return dto;
                            }
                    )
            ));
            dtos.addAll(productCustomerWarehouseSummary.values());
        }
        //移除数量为0的数据
        dtos.removeIf(item -> item.getQuantity() == 0);
        //返回分页数据
        return getSalesReportItemDTOPageResults(page, dtos);
    }


    @Data
    public static class Query {
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
            if (StringUtils.isNotBlank(filter)) {
                builder.and(qSalesOutbound.orderNo.like("%" + filter + "%"));
                //whereClause.append(" AND so.order_no LIKE '%").append(":filter").append("%'");
                whereClause.append(" AND so.order_no LIKE :filter");
                params.put("filter", filter);

            }
        }

        public void setState(String state) {
            if (StringUtils.isNotBlank(state)) {
                builder.and(qSalesOutbound.orderStatus.eq(OrderStatus.valueOf(state)));
                whereClause.append(" AND so.order_status = :state");
                params.put("state", state);

            }
        }



        public void setStart(String start) {
            if (StringUtils.isNotBlank(start)) {
                whereClause.append(" AND so.outbound_date >= :start");
                params.put("start", start);
            }
        }

        public void setEnd(String end) {
            if (StringUtils.isNotBlank(end)) {
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
