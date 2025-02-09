package com.flyemu.share.service.sales;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.SalesOutboundDTO;
import com.flyemu.share.dto.SalesOutboundItemDTO;
import com.flyemu.share.dto.SalesReturnDTO;
import com.flyemu.share.dto.SalesReturnItemDTO;
import com.flyemu.share.entity.basic.QCustomer;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QUnit;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.sales.*;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesReturnForm;
import com.flyemu.share.repository.SalesOutboundItemRepository;
import com.flyemu.share.repository.SalesOutboundRepository;
import com.flyemu.share.repository.SalesReturnItemRepository;
import com.flyemu.share.repository.SalesReturnRepository;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


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
public class SalesReturnService extends AbsService {

    private final static QSalesReturn qSalesReturn = QSalesReturn.salesReturn;
    private final static QSalesReturnItem qsalesReturnItem = QSalesReturnItem.salesReturnItem;

    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;

    private final static QCustomer qCustomer = QCustomer.customer;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QProduct qProduct = QProduct.product;
    private final static QUnit qUnit = QUnit.unit;


    private final SalesReturnRepository salesReturnRepository;
    private final SalesReturnItemRepository salesReturnItemRepository;
    private final CodeSeedService codeSeedService;

    private final SalesOutboundRepository salesOutboundRepository;
    private final SalesOutboundItemRepository salesOutboundItemRepository;

    public PageResults<SalesReturnDTO> query(Page page, SalesReturnService.Query query) {
        long totalSize = bqf.selectFrom(qSalesReturn)
                .where(query.builder)
                .fetchCount();

        List<Tuple> fetchPage = bqf.selectFrom(qSalesReturn)
                .select(qSalesReturn, qCustomer.name, qMerchantUser.name)
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesReturn.createdBy))
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesReturn.customerId))
                .where(query.builder)
                .orderBy(qSalesReturn.id.desc())
                .offset(page.getOffset())
                .limit(page.getOffsetEnd())
                .fetch();

        List<SalesReturnDTO> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            SalesReturnDTO salesReturnDTO = BeanUtil.toBean(tuple.get(qSalesReturn), SalesReturnDTO.class);
            salesReturnDTO.setCustomerName(tuple.get(qCustomer.name));
            salesReturnDTO.setCreatedName(tuple.get(qMerchantUser.name));
            //查询子表
            List<SalesReturnItem> salesReturnItemList = bqf.selectFrom(qsalesReturnItem)
                    .select(qsalesReturnItem)
                    .where(qsalesReturnItem.salesReturnId.eq(salesReturnDTO.getId()))
                    .fetch();
            List<SalesReturnItemDTO> itemDTOs = new ArrayList<>();
            salesReturnItemList.forEach(item -> {
                SalesReturnItemDTO itemDTO = BeanUtil.toBean(item, SalesReturnItemDTO.class);
                itemDTOs.add(itemDTO);
            });
            salesReturnDTO.setSalesReturnItemList(itemDTOs);

            //查询关联的出库单
            List<String> salesOutboundList = bqf.selectFrom(qSalesOutbound)
                    .select(qSalesOutbound.orderNo)
                    .where(qSalesOutbound.returnOrderId.eq(salesReturnDTO.getId()))
                    .fetch();
            if(!CollectionUtils.isEmpty(salesOutboundList)){
                salesReturnDTO.setSalesOutboundNos(String.join(",", salesOutboundList));
            }

            dtos.add(salesReturnDTO);
        });

        return new PageResults<>(dtos, page, totalSize);
    }

    @Transactional
    public SalesReturn save(SalesReturnForm salesReturnForm) {
        SalesReturn salesReturn = salesReturnForm.getSalesReturn();
        List<SalesReturnItem> salesReturnItemList = salesReturnForm.getSalesReturnItemList();
        Long id = salesReturn.getId();
        if (id != null) {
            //更新
            SalesReturn original = salesReturnRepository.getById(salesReturn.getId());
            BeanUtil.copyProperties(salesReturn, original, CopyOptions.create().ignoreNullValue());
            SalesReturn update = salesReturnRepository.save(original);
            if (!CollectionUtils.isEmpty(salesReturnItemList)) {
                salesReturnItemList.forEach(item -> {
                    item.setAccountBookId(salesReturn.getAccountBookId());
                    item.setMerchantId(salesReturn.getMerchantId());
                });
                //批量修改
                salesReturnItemRepository.saveAll(salesReturnItemList);
            }
            return update;
        }else{
            //状态初始化
            salesReturn.setOrderStatus(OrderStatus.已保存);
            //订单编号
            salesReturn.setOrderNo(codeSeedService.generateCode(salesReturn.getMerchantId(), "销售退货单"));
            SalesReturn save = salesReturnRepository.save(salesReturn);
            if (!CollectionUtils.isEmpty(salesReturnItemList)) {
                salesReturnItemList.forEach(item -> {
                    item.setSalesReturnId(save.getId());
                    item.setAccountBookId(salesReturn.getAccountBookId());
                    item.setMerchantId(salesReturn.getMerchantId());
                    item.setCreatedBy(salesReturn.getCreatedBy());
                    item.setCreatedAt(salesReturn.getCreatedAt());
                });
                //批量保存
                salesReturnItemRepository.saveAll(salesReturnItemList);
            }
            //选择的源单不为空
            List<Long> selectSalesOutboundIdList = salesReturnForm.getSelectSalesOutboundIdList();
            if(!CollectionUtils.isEmpty(selectSalesOutboundIdList)){
                List<Long> collect = selectSalesOutboundIdList.stream().distinct().toList();
                List<SalesOutbound> salesOutboundList = salesOutboundRepository.findAllById(collect);
                salesOutboundList.forEach(order -> {
                    //退货单 关联 销售出库单
                    order.setReturnOrderId(save.getId());
                });
                salesOutboundRepository.saveAll(salesOutboundList);
            }
            return save;
        }

    }

    @Transactional
    public void delete(Long salesReturnId, Long merchantId, Long accountBookId) {
        jqf.delete(qSalesReturn)
                .where(qSalesReturn.id.eq(salesReturnId).and(qSalesReturn.merchantId.eq(merchantId)).and(qSalesReturn.accountBookId.eq(accountBookId)))
                .execute();

        //删除退货单商品
        jqf.delete(qsalesReturnItem)
                .where(qsalesReturnItem.salesReturnId.eq(salesReturnId).and(qsalesReturnItem.merchantId.eq(merchantId)).and(qsalesReturnItem.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<SalesReturn> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSalesReturn).where(qSalesReturn.merchantId.eq(merchantId).and(qSalesReturn.accountBookId.eq(accountBookId))).fetch();
    }

    public Object getById(SalesReturn query) {
        //查询订单
        SalesReturn salesReturn = salesReturnRepository.getById(query.getId());
        //订单数据转换
        SalesReturnDTO dto = BeanUtil.toBean(salesReturn, SalesReturnDTO.class);
        //查询销售订单商品
        List<Tuple> fetch = jqf.selectFrom(qsalesReturnItem)
                .select(qsalesReturnItem, qProduct.code, qProduct.name, qUnit.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qsalesReturnItem.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qsalesReturnItem.baseUnitId))
                .where(qsalesReturnItem.salesReturnId.eq(query.getId())).orderBy(qsalesReturnItem.id.asc()).fetch();
        List<SalesReturnItemDTO> salesReturnItemDTOList = new ArrayList<>();
        fetch.forEach(tuple -> {
            SalesReturnItemDTO salesReturnItemDTO = BeanUtil.toBean(tuple.get(qsalesReturnItem), SalesReturnItemDTO.class);
            salesReturnItemDTO.setProductName(tuple.get(qProduct.name));
            salesReturnItemDTO.setProductCode(tuple.get(qProduct.code));
            salesReturnItemDTO.setUnitName(tuple.get(qUnit.name));
            salesReturnItemDTOList.add(salesReturnItemDTO);
        });
        dto.setSalesReturnItemList(salesReturnItemDTOList);
        return dto;
    }

    @Transactional
    public void batchAudit(SalesReturnForm salesReturnForm) {
        List<Long> orderIds = salesReturnForm.getOrderIds();
        if (orderIds == null || orderIds.isEmpty()) {
            throw new IllegalArgumentException("Order IDs cannot be null or empty");
        }

        List<SalesReturn> salesReturnList = salesReturnRepository.findAllById(orderIds);

        if (salesReturnList.size() != orderIds.size()) {
            throw new IllegalArgumentException("Some salesOutboundList could not be found");
        }
        SalesReturn salesReturn = salesReturnForm.getSalesReturn();
        salesReturnList.forEach(order -> {
            order.setOrderStatus(OrderStatus.已审核);
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovedBy(salesReturn.getApprovedBy());
        });
        salesReturnRepository.saveAll(salesReturnList);
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSalesReturn.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSalesReturn.accountBookId.eq(accountBookId));
            }
        }
    }
}
