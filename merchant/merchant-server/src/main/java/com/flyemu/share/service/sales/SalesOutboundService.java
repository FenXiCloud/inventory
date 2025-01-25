package com.flyemu.share.service.sales;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.SalesOrderDTO;
import com.flyemu.share.dto.SalesOrderItemDTO;
import com.flyemu.share.dto.SalesOutboundDTO;
import com.flyemu.share.dto.SalesOutboundItemDTO;
import com.flyemu.share.entity.basic.QCustomer;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QUnit;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.sales.*;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.form.SalesOutboundForm;
import com.flyemu.share.repository.SalesOrderRepository;
import com.flyemu.share.repository.SalesOutboundItemRepository;
import com.flyemu.share.repository.SalesOutboundRepository;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.flyemu.share.entity.sales.QSalesOrder.salesOrder;

/**
 * @功能描述: 销售出库单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesOutboundService extends AbsService {

    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private final static QSalesOutboundItem qSalesOutboundItem = QSalesOutboundItem.salesOutboundItem;

    private final static QCustomer qCustomer = QCustomer.customer;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QProduct qProduct = QProduct.product;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final static QUnit qUnit = QUnit.unit;

    private final SalesOutboundRepository salesOutboundRepository;
    private final SalesOutboundItemRepository salesOutboundItemRepository;
    private final CodeSeedService codeSeedService;
    private final static QSalesOrder qSalesOrder = QSalesOrder.salesOrder;
    private final SalesOrderRepository salesOrderRepository;

    public PageResults<SalesOutboundDTO> query(Page page, SalesOutboundService.Query query) {
        PagedList<Tuple> tuples = bqf.selectFrom(qSalesOutbound)
                .select(qSalesOutbound, qCustomer.name, qMerchantUser.name)
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesOutbound.createdBy))
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOutbound.customerId))
                .where(query.builder).orderBy(qSalesOutbound.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<SalesOutboundDTO> dtos = new ArrayList<>();
        tuples.forEach(tuple -> {
            SalesOutboundDTO salesOutboundDTO = BeanUtil.toBean(tuple.get(qSalesOutbound), SalesOutboundDTO.class);
            salesOutboundDTO.setCustomerName(tuple.get(qCustomer.name));
            salesOutboundDTO.setCreatedName(tuple.get(qMerchantUser.name));
            dtos.add(salesOutboundDTO);
        });

        return new PageResults<>(dtos, page, tuples.getTotalSize());
    }

    @Transactional
    public SalesOutbound save(SalesOutboundForm salesOutboundForm) {
        SalesOutbound salesOutbound = salesOutboundForm.getSalesOutbound();
        Long id = salesOutbound.getId();
        List<SalesOutboundItem> salesOutboundItemList = salesOutboundForm.getSalesOutboundItemList();
        // todo 根据产品id和仓库id 查询库存服务是否有库存;
        if (id != null) {
            //查询
            SalesOutbound original = salesOutboundRepository.getById(id);
            BeanUtil.copyProperties(salesOutbound, original, CopyOptions.create().ignoreNullValue());
            //修改
            SalesOutbound update = salesOutboundRepository.save(original);
            if (!CollectionUtils.isEmpty(salesOutboundItemList)) {
                //批量修改
                salesOutboundItemRepository.saveAll(salesOutboundItemList);
            }
            return update;
        }else{
            //状态初始化
            salesOutbound.setOrderStatus(OrderStatus.已保存);
            //订单编号
            salesOutbound.setOrderNo(codeSeedService.generateCode(salesOutbound.getMerchantId(), "销售出库单"));
            //保存订单
            SalesOutbound save = salesOutboundRepository.save(salesOutbound);
            if (!CollectionUtils.isEmpty(salesOutboundItemList)) {
                salesOutboundItemList.forEach(item -> {
                    item.setSalesOutboundId(save.getId());
                    item.setAccountBookId(salesOutbound.getAccountBookId());
                    item.setMerchantId(salesOutbound.getMerchantId());
                    item.setCreatedBy(salesOutbound.getCreatedBy());
                    item.setCreatedAt(salesOutbound.getCreatedAt());
                });
                //批量保存
                salesOutboundItemRepository.saveAll(salesOutboundItemList);
            }
            //选择的源单不为空
            List<Long> selectSalesOrderIdList = salesOutboundForm.getSelectSalesOrderIdList();
            if(!CollectionUtils.isEmpty(selectSalesOrderIdList)){
                List<Long> collect = selectSalesOrderIdList.stream().distinct().toList();
                List<SalesOrder> salesOrderList = salesOrderRepository.findAllById(collect);
                salesOrderList.forEach(order -> {
                    //销售订单关联销售出库单
                    order.setOutOrderId(save.getId());
                });
                salesOrderRepository.saveAll(salesOrderList);
            }
            return save;
        }
    }

    @Transactional
    public void delete(Long salesOutboundId, Long merchantId, Long accountBookId) {
        jqf.delete(qSalesOutbound)
                .where(qSalesOutbound.id.eq(salesOutboundId).and(qSalesOutbound.merchantId.eq(merchantId)).and(qSalesOutbound.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<SalesOutbound> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSalesOutbound).where(qSalesOutbound.merchantId.eq(merchantId).and(qSalesOutbound.accountBookId.eq(accountBookId))).fetch();
    }

    public Object getById(SalesOrder query) {
        //查询订单
        SalesOutbound salesOutbound = salesOutboundRepository.getById(query.getId());
        //订单数据转换
        SalesOutboundDTO dto = BeanUtil.toBean(salesOutbound, SalesOutboundDTO.class);
        //查询销售订单商品
        List<Tuple> fetch = jqf.selectFrom(qSalesOutboundItem)
                .select(qSalesOutboundItem, qProduct.code, qProduct.name, qUnit.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qSalesOutboundItem.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qSalesOutboundItem.baseUnitId))
                .where(qSalesOutboundItem.salesOutboundId.eq(query.getId())).orderBy(qSalesOutboundItem.id.asc()).fetch();
        List<SalesOutboundItemDTO> salesOutboundItemDTOList = new ArrayList<>();
        fetch.forEach(tuple -> {
            SalesOutboundItemDTO salesOutboundItemDTO = BeanUtil.toBean(tuple.get(qSalesOutboundItem), SalesOutboundItemDTO.class);
            salesOutboundItemDTO.setProductName(tuple.get(qProduct.name));
            salesOutboundItemDTO.setProductCode(tuple.get(qProduct.code));
            salesOutboundItemDTO.setUnitName(tuple.get(qUnit.name));
            salesOutboundItemDTOList.add(salesOutboundItemDTO);
        });
        dto.setSalesOutboundItemList(salesOutboundItemDTOList);
        return dto;
    }

    @Transactional
    public void batchAudit(SalesOutboundForm salesOutboundForm) {
        List<Long> orderIds = salesOutboundForm.getOrderIds();
        if (orderIds == null || orderIds.isEmpty()) {
            throw new IllegalArgumentException("Order IDs cannot be null or empty");
        }

        List<SalesOutbound> salesOutboundList = salesOutboundRepository.findAllById(orderIds);

        if (salesOutboundList.size() != orderIds.size()) {
            throw new IllegalArgumentException("Some salesOutboundList could not be found");
        }
        SalesOutbound salesOutbound = salesOutboundForm.getSalesOutbound();
        salesOutboundList.forEach(order -> {
            order.setOrderStatus(OrderStatus.已审核);
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovedBy(salesOutbound.getApprovedBy());
        });

        salesOutboundRepository.saveAll(salesOutboundList);
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSalesOutbound.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSalesOutbound.accountBookId.eq(accountBookId));
            }
        }
        public void setFilter(String filter) {
            if (StringUtils.isNotBlank(filter)) {
                builder.and(qSalesOutbound.orderNo.like("%" + filter + "%"));
            }
        }

        public void setState(String state) {
            if (StringUtils.isNotBlank(state)) {
                builder.and(qSalesOutbound.orderStatus.eq(OrderStatus.valueOf(state)));
            }
        }

        public void setStart(String start) {
            if (StringUtils.isNotBlank(start)) {
                builder.and(qSalesOutbound.outboundDate.goe(LocalDate.parse(start)));
            }
        }

        public void setEnd(String end) {
            if (StringUtils.isNotBlank(end)) {
                builder.and(qSalesOutbound.outboundDate.loe(LocalDate.parse(end)));
            }
        }

        public void setCustomerId(Long customerId) {
            if (customerId != null) {
                builder.and(qSalesOutbound.customerId.eq(customerId));
            }
        }
    }
}
