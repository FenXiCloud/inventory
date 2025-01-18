package com.flyemu.share.service.sales;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.sales.QSalesOrder;
import com.flyemu.share.entity.sales.QSalesOrderItem;
import com.flyemu.share.entity.sales.SalesOrder;
import com.flyemu.share.entity.sales.SalesOrderItem;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.repository.PurchaseOrderItemRepository;
import com.flyemu.share.repository.SalesOrderItemRepository;
import com.flyemu.share.repository.SalesOrderRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 销售订单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesOrderService extends AbsService {

    private final static QSalesOrder qSalesOrder = QSalesOrder.salesOrder;
    private final static QSalesOrderItem qSalesOrderItem = QSalesOrderItem.salesOrderItem;

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;

    public PageResults<SalesOrder> query(Page page, SalesOrderService.Query query) {
        PagedList<SalesOrder> fetchPage = bqf.selectFrom(qSalesOrder).where(query.builder).orderBy(qSalesOrder.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<SalesOrder> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            SalesOrder salesOrder1 = tuple;
            SalesOrder salesOrder = BeanUtil.toBean(salesOrder1, SalesOrder.class);
            dtos.add(salesOrder);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public SalesOrder save(SalesOrderForm salesOrderForm) {
        SalesOrder salesOrder = salesOrderForm.getSalesOrder();
        Long id = salesOrder.getId();
        List<SalesOrderItem> salesOrderItemList = salesOrderForm.getSalesOrderItemList();
        if (id != null) {
            //查询
            SalesOrder original = salesOrderRepository.getById(id);
            //租户隔离
            Long merchantId = original.getMerchantId();
            Long accountBookId = original.getAccountBookId();
            if(!salesOrder.getAccountBookId().equals(accountBookId) || !salesOrder.getMerchantId().equals(merchantId)){
                throw new RuntimeException("参数错误!!");
            }
            //修改销售订单
            SalesOrder update = salesOrderRepository.save(original);
            if (!CollectionUtils.isEmpty(salesOrderItemList)) {
                //批量修改销售订单商品
                salesOrderItemRepository.saveAll(salesOrderItemList);
            }
            return update;
        }else{
            salesOrder.setOrderStatus(OrderStatus.已保存);
            //保存销售订单
            SalesOrder save = salesOrderRepository.save(salesOrder);
            if (!CollectionUtils.isEmpty(salesOrderItemList)) {
                salesOrderItemList.forEach(item -> {
                    item.setAccountBookId(salesOrder.getAccountBookId());
                    item.setMerchantId(salesOrder.getMerchantId());
                    item.setCreatedBy(salesOrder.getCreatedBy());
                    item.setCreatedAt(salesOrder.getCreatedAt());
                });
                //批量添加销售订单商品
                salesOrderItemRepository.saveAll(salesOrderItemList);
            }
            return save;
        }
    }

    @Transactional
    public void delete(Long SalesOrderId, Long merchantId, Long accountBookId) {
        //删除销售订单
        jqf.delete(qSalesOrder)
                .where(qSalesOrder.id.eq(SalesOrderId).and(qSalesOrder.merchantId.eq(merchantId)).and(qSalesOrder.accountBookId.eq(accountBookId)))
                .execute();

        //删除销售订单商品
        jqf.delete(qSalesOrderItem)
                .where(qSalesOrderItem.salesOrderId.eq(SalesOrderId).and(qSalesOrderItem.merchantId.eq(merchantId)).and(qSalesOrderItem.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<SalesOrder> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSalesOrder).where(qSalesOrder.merchantId.eq(merchantId).and(qSalesOrder.accountBookId.eq(accountBookId))).fetch();
    }

    public SalesOrder getById(SalesOrder query) {
        SalesOrder salesOrder = salesOrderRepository.getById(query.getId());
        return salesOrder;
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSalesOrder.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSalesOrder.accountBookId.eq(accountBookId));
            }
        }
    }
}
