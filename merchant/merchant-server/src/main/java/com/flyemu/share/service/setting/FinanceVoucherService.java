package com.flyemu.share.service.setting;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.dto.FinanceVoucherCandidateVO;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.AcDetailsDto;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.VoucherDetailsDto;
import com.flyemu.share.dto.VoucherDto;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.purchase.*;
import com.flyemu.share.entity.sales.*;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.setting.*;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.FinanceVoucherForm;
import com.flyemu.share.repository.setting.FinanceVoucherRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.BooleanBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinanceVoucherService extends BaseService {

    private final static QFinanceVoucher qFinanceVoucher = QFinanceVoucher.financeVoucher;

    private final FinanceVoucherRepository financeVoucherRepository;

    private final FinanceAccountLinkService financeAccountLinkService;

    private final FinanceVoucherTemplateService financeVoucherTemplateService;

    private final FinanceItemMapService financeItemMapService;

    private final static QProduct qProduct = QProduct.product;
    private final static QCustomer qCustomer = QCustomer.customer;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QPurchaseOrder qPurchaseOrder = QPurchaseOrder.purchaseOrder;
    private final static QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;
    private final static QPurchaseReturn qPurchaseReturn = QPurchaseReturn.purchaseReturn;
    private final static QSalesOrder qSalesOrder = QSalesOrder.salesOrder;
    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private final static QSalesReturn qSalesReturn = QSalesReturn.salesReturn;
    private final static QInventoryTransfer qInventoryTransfer = QInventoryTransfer.inventoryTransfer;
    private final static QStockTake qStockTake = QStockTake.stockTake;
    private final static QOtherInbound qOtherInbound = QOtherInbound.otherInbound;
    private final static QOtherOutbound qOtherOutbound = QOtherOutbound.otherOutbound;
    private final static QCostAdjustment qCostAdjustment = QCostAdjustment.costAdjustment;
    private final static QOrderReceipt qOrderReceipt = QOrderReceipt.orderReceipt;
    private final static QOrderPayment qOrderPayment = QOrderPayment.orderPayment;
    private final static QVerification qVerification = QVerification.verification;
    private final static QVerificationItem qVerificationItem = QVerificationItem.verificationItem;
    private final static QOtherReceipt qOtherReceipt = QOtherReceipt.otherReceipt;
    private final static QOtherExpense qOtherExpense = QOtherExpense.otherExpense;
    private final static QAccountTransfer qAccountTransfer = QAccountTransfer.accountTransfer;

    public List<FinanceVoucher> query(FinanceVoucherService.Query query) {
        return bqf.selectFrom(qFinanceVoucher)
                .where(query.builder)
                .where(query.builders())
                .orderBy(qFinanceVoucher.id.desc())
                .fetch();
    }

    @Transactional
    public void save(FinanceVoucherForm financeVoucherForm) throws JsonProcessingException, UnsupportedEncodingException {
        String type = financeVoucherForm.getType();
        BigDecimal amount = financeVoucherForm.getAmount();
        Long orderId = financeVoucherForm.getOrderId();
        Long accountBookId = financeVoucherForm.getAccountBookId();
        FinanceAccountLink financeAccountLink = financeAccountLinkService.loadByAccountBookId(accountBookId);
        if (financeAccountLink == null) {
            throw new ServiceException("未配置关联财务云软件～");
        }
        FinanceVoucherTemplate financeVoucherTemplate = financeVoucherTemplateService.findByType(type);
        if (financeVoucherTemplate == null) {
            throw new ServiceException("未配置对应凭证模板～");
        }
        VoucherDto voucherDto = new VoucherDto();
        voucherDto.setCreditAmount(amount);
        voucherDto.setDebitAmount(amount);
        voucherDto.setVoucherDate(financeVoucherForm.getOrderTime());
        voucherDto.setCreateDate(LocalDateTime.now());
        voucherDto.setWord(financeVoucherTemplate.getWord());
        voucherDto.setRemark(financeVoucherForm.getRemark());
        List<VoucherDetailsDto> detailsDtos = new ArrayList<>();
        JSONArray details = financeVoucherTemplate.getDetails();
        VoucherDetailsDto voucherDetailsDto;
        for (int i = 0; i < details.size(); i++) {
            voucherDetailsDto = new VoucherDetailsDto();
            voucherDetailsDto.setSummary(financeVoucherForm.getOrderName());
            JSONObject jsonObject = details.getJSONObject(i);
            JSONArray auxiliaryAccounting = jsonObject.getJSONArray("auxiliaryAccounting");
            if (auxiliaryAccounting == null) {
                auxiliaryAccounting = new JSONArray();
            }
            List<AcDetailsDto> auxiliaryAccountingList = new ArrayList<>();
            AcDetailsDto acDetailsDto;
            for (int j = 0; j < auxiliaryAccounting.size(); j++) {
                String categoryId = auxiliaryAccounting.getString(0);
                Long inventoryId = -1L;
                String msgTips = "";
                String name = "";
                switch (categoryId.toString()) {
                    case "0": {
                        inventoryId = financeVoucherForm.getCustomerId();
                        msgTips = "客户";
                        Customer customer = jqf.selectFrom(qCustomer).where(qCustomer.id.eq(inventoryId)).fetchOne();
                        name = customer.getName();
                        break;
                    }
                    case "1": {
                        inventoryId = financeVoucherForm.getSupplierId();
                        msgTips = "供应商";
                        Supplier supplier = jqf.selectFrom(qSupplier).where(qSupplier.id.eq(inventoryId)).fetchOne();
                        name = supplier.getName();
                        break;
                    }
                    case "6": {
                        inventoryId = financeVoucherForm.getProductId();
                        Product product = jqf.selectFrom(qProduct).where(qProduct.id.eq(inventoryId)).fetchOne();
                        name = product.getName();
                        msgTips = "商品";
                        break;
                    }
                    default:
                        break;
                }
                FinanceItemMap financeItemMap = financeItemMapService.findByCategoryIdAndInventoryId(categoryId, inventoryId);
                if (financeItemMap == null) {
                    throw new ServiceException("未配置“" + msgTips + "”辅助映射～");
                }
                acDetailsDto = new AcDetailsDto();
                acDetailsDto.setAccountSetsId(financeAccountLink.getFinanceAccountId());
                acDetailsDto.setCategoryId(financeItemMap.getCategoryId());
                acDetailsDto.setCategoryName(financeItemMap.getCategoryName());
                acDetailsDto.setCategoryType(financeItemMap.getCategoryType());
                acDetailsDto.setCode(financeItemMap.getFinanceCode());
                acDetailsDto.setId(financeItemMap.getFinanceId());
                acDetailsDto.setAccountSetsId(financeAccountLink.getFinanceAccountId());
                acDetailsDto.setEnable(true);
                acDetailsDto.setName(name);
                acDetailsDto.setCreateTime(LocalDateTime.now());
                auxiliaryAccountingList.add(acDetailsDto);
            }
            Long subjectId = jsonObject.getLong("subjectId");
            String subjectName = jsonObject.getString("subjectName");
            String subjectCode = jsonObject.getString("subjectCode");
            voucherDetailsDto.setSubjectId(subjectId);
            voucherDetailsDto.setSubjectCode(subjectCode);
            voucherDetailsDto.setSubjectName(subjectName);
            if ((i + 1) % 2 == 0) {
                voucherDetailsDto.setCreditAmount(amount);
            } else {
                voucherDetailsDto.setDebitAmount(amount);
            }
            voucherDetailsDto.setAuxiliary(auxiliaryAccountingList);
            detailsDtos.add(voucherDetailsDto);
        }
        voucherDto.setDetails(detailsDtos);
        voucherDto.setInsert(true);
        voucherDto.setIsTemplate(false);
        Integer wordCode = financeAccountLinkService.loadWordCode(accountBookId, voucherDto.getWord());
        voucherDto.setCode(wordCode);
        JSONObject jsonObject = financeAccountLinkService.createVoucher(accountBookId, voucherDto);
        log.info(jsonObject.toJSONString());
        String code = jsonObject.getString("code");
        String voucherId = jsonObject.getString("id");
        FinanceVoucher financeVoucher = new FinanceVoucher();
        financeVoucher.setCode(code);
        financeVoucher.setVoucherId(voucherId);
        financeVoucher.setAccountBookId(accountBookId);
        financeVoucher.setOrderId(orderId);
        financeVoucher.setParams(JSONObject.toJSONString(voucherDto));
        financeVoucher.setAccountBookId(accountBookId);
        financeVoucher.setCreatedAt(LocalDateTime.now());
        financeVoucher.setMerchantId(financeVoucherForm.getMerchantId());
        financeVoucher.setType(type);
        financeVoucher.setCreatedBy(financeVoucherTemplate.getCreatedBy());
        financeVoucherRepository.save(financeVoucher);
    }

    @Transactional
    public void delete(Long id, Long merchantId, Long accountBookId) {
        jqf.delete(qFinanceVoucher)
                .where(qFinanceVoucher.id.eq(id).and(qFinanceVoucher.merchantId.eq(merchantId)).and(qFinanceVoucher.accountBookId.eq(accountBookId)))
                .execute();
    }

    public FinanceVoucher load(Long merchantId, Long id) {
        return jqf.selectFrom(qFinanceVoucher)
                .where(qFinanceVoucher.merchantId.eq(merchantId).and(qFinanceVoucher.id.eq(id)))
                .fetchOne();
    }

    public void upVoucher(VoucherDto voucherDto, AccountDto accountDto) {
        FinanceAccountLink financeAccountLink = financeAccountLinkService.loadByAccountBookId(accountDto.getAccountBookId());
        if (financeAccountLink == null) {
            throw new ServiceException("未配置关联财务云软件～");
        }
        financeAccountLinkService.upVoucher(financeAccountLink, voucherDto);
    }

    public Double balance(String subjectId, String categoryId, String categoryDetailsId, AccountDto accountDto) {
        FinanceAccountLink financeAccountLink = financeAccountLinkService.loadByAccountBookId(accountDto.getAccountBookId());
        if (financeAccountLink == null) {
            throw new ServiceException("未配置关联财务云软件～");
        }
        return financeAccountLinkService.balance(subjectId, categoryId, categoryDetailsId, accountDto);
    }

    public Object loadAuxiliaryAccountingData(List<String> ids, AccountDto accountDto) {
        FinanceAccountLink financeAccountLink = financeAccountLinkService.loadByAccountBookId(accountDto.getAccountBookId());
        if (financeAccountLink == null) {
            throw new ServiceException("未配置关联财务云软件～");
        }
        return financeAccountLinkService.loadAuxiliaryAccountingData(ids, accountDto);
    }

    public Object loadVoucher(String voucherId, AccountDto accountDto) {
        FinanceAccountLink financeAccountLink = financeAccountLinkService.loadByAccountBookId(accountDto.getAccountBookId());
        if (financeAccountLink == null) {
            throw new ServiceException("未配置关联财务云软件～");
        }
        return financeAccountLinkService.loadVoucher(voucherId, accountDto);
    }

    public PageResults<FinanceVoucherCandidateVO> candidates(Page page, CandidateQuery query) {
        String documentType = query.getDocumentType();
        if (documentType == null || documentType.isBlank()) {
            throw new ServiceException("单据类型不能为空");
        }
        return switch (documentType) {
            case "采购订单" -> candidatesPurchaseOrder(page, query);
            case "采购入库单" -> candidatesPurchaseInbound(page, query);
            case "采购退货单" -> candidatesPurchaseReturn(page, query);
            case "销售订单" -> candidatesSalesOrder(page, query);
            case "销售出库单" -> candidatesSalesOutbound(page, query);
            case "销售退货单" -> candidatesSalesReturn(page, query);
            case "调拨单" -> candidatesInventoryTransfer(page, query);
            case "盘点单" -> candidatesStockTake(page, query);
            case "其他入库单" -> candidatesOtherInbound(page, query);
            case "其他出库单" -> candidatesOtherOutbound(page, query);
            case "成本调整单" -> candidatesCostAdjustment(page, query);
            case "收款单" -> candidatesOrderReceipt(page, query);
            case "付款单" -> candidatesOrderPayment(page, query);
            case "核销单" -> candidatesVerification(page, query);
            case "其他收款单" -> candidatesOtherReceipt(page, query);
            case "其他付款单" -> candidatesOtherExpense(page, query);
            case "转帐单" -> candidatesAccountTransfer(page, query);
            default -> throw new ServiceException("不支持的单据类型：" + documentType);
        };
    }

    private BooleanBuilder voucherJoinOn(com.querydsl.core.types.dsl.NumberPath<Long> orderId, String type, Long merchantId, Long accountBookId) {
        return new BooleanBuilder()
                .and(qFinanceVoucher.orderId.eq(orderId))
                .and(qFinanceVoucher.type.eq(type))
                .and(qFinanceVoucher.merchantId.eq(merchantId))
                .and(qFinanceVoucher.accountBookId.eq(accountBookId));
    }

    private void applyExclude(BooleanBuilder builder, CandidateQuery query) {
        if (query.isExcludeVouchered()) {
            builder.and(qFinanceVoucher.id.isNull());
        }
    }

    private void applyLocalDateRange(BooleanBuilder builder, com.querydsl.core.types.dsl.DatePath<LocalDate> datePath, CandidateQuery query) {
        if (query.getStartDate() != null) {
            builder.and(datePath.goe(query.getStartDate()));
        }
        if (query.getEndDate() != null) {
            builder.and(datePath.loe(query.getEndDate()));
        }
    }

    private Date toDate(LocalDate localDate) {
        return localDate == null ? null : java.sql.Date.valueOf(localDate);
    }

    private void applyUtilDateRange(BooleanBuilder builder, com.querydsl.core.types.dsl.DateTimePath<Date> datePath, CandidateQuery query) {
        if (query.getStartDate() != null) {
            builder.and(datePath.goe(toDate(query.getStartDate())));
        }
        if (query.getEndDate() != null) {
            builder.and(datePath.loe(toDate(query.getEndDate())));
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof LocalDateTime ldt) return ldt.toLocalDate();
        if (value instanceof java.sql.Date sd) return sd.toLocalDate();
        if (value instanceof Date d) return new java.sql.Date(d.getTime()).toLocalDate();
        return null;
    }

    private FinanceVoucherCandidateVO toCandidate(Long id, String orderNo, LocalDate orderDate, String documentType,
                                                  java.math.BigDecimal amount, String customerName, String supplierName,
                                                  String createName, String voucherCode, Long financeVoucherId,
                                                  Long customerId, Long supplierId) {
        FinanceVoucherCandidateVO vo = new FinanceVoucherCandidateVO();
        vo.setId(id);
        vo.setOrderId(id);
        vo.setOrderNo(orderNo);
        vo.setOrderDate(orderDate);
        vo.setDocumentType(documentType);
        vo.setAmount(amount);
        vo.setCustomerName(customerName);
        vo.setSupplierName(supplierName);
        vo.setCreateName(createName);
        vo.setVoucherCode(voucherCode);
        vo.setVoucherId(financeVoucherId);
        vo.setFinanceVoucherId(financeVoucherId);
        vo.setCustomerId(customerId);
        vo.setSupplierId(supplierId);
        String orderName = orderNo;
        if (orderName == null || orderName.isBlank()) {
            orderName = customerName != null ? customerName : supplierName;
        }
        vo.setOrderName(orderName);
        return vo;
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesPurchaseOrder(Page page, CandidateQuery query) {
        String type = "采购订单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qPurchaseOrder.merchantId.eq(query.getMerchantId()))
                .and(qPurchaseOrder.accountBookId.eq(query.getAccountBookId()))
                .and(qPurchaseOrder.orderStatus.eq(OrderStatus.已审核));
        applyLocalDateRange(builder, qPurchaseOrder.orderDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qPurchaseOrder)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qPurchaseOrder.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qPurchaseOrder)
                .select(qPurchaseOrder, qSupplier.name, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseOrder.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseOrder.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qPurchaseOrder.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qPurchaseOrder.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            PurchaseOrder o = t.get(qPurchaseOrder);
            list.add(toCandidate(o.getId(), o.getOrderNo(), o.getOrderDate(), type, o.getFinalAmount(),
                    null, t.get(qSupplier.name), t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), null, o.getSupplierId()));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesPurchaseInbound(Page page, CandidateQuery query) {
        String type = "采购入库单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qPurchaseInbound.merchantId.eq(query.getMerchantId()))
                .and(qPurchaseInbound.accountBookId.eq(query.getAccountBookId()))
                .and(qPurchaseInbound.orderStatus.eq(OrderStatus.已审核));
        applyLocalDateRange(builder, qPurchaseInbound.inboundDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qPurchaseInbound)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qPurchaseInbound.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qPurchaseInbound)
                .select(qPurchaseInbound, qSupplier.name, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseInbound.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qPurchaseInbound.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qPurchaseInbound.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            PurchaseInbound o = t.get(qPurchaseInbound);
            list.add(toCandidate(o.getId(), o.getOrderNo(), o.getInboundDate(), type, o.getFinalAmount(),
                    null, t.get(qSupplier.name), t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), null, o.getSupplierId()));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesPurchaseReturn(Page page, CandidateQuery query) {
        String type = "采购退货单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qPurchaseReturn.merchantId.eq(query.getMerchantId()))
                .and(qPurchaseReturn.accountBookId.eq(query.getAccountBookId()))
                .and(qPurchaseReturn.orderStatus.eq(OrderStatus.已审核));
        applyLocalDateRange(builder, qPurchaseReturn.returnDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qPurchaseReturn)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qPurchaseReturn.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qPurchaseReturn)
                .select(qPurchaseReturn, qSupplier.name, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseReturn.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseReturn.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qPurchaseReturn.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qPurchaseReturn.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            PurchaseReturn o = t.get(qPurchaseReturn);
            list.add(toCandidate(o.getId(), o.getOrderNo(), o.getReturnDate(), type, o.getRefundAmount(),
                    null, t.get(qSupplier.name), t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), null, o.getSupplierId()));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesSalesOrder(Page page, CandidateQuery query) {
        String type = "销售订单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qSalesOrder.merchantId.eq(query.getMerchantId()))
                .and(qSalesOrder.accountBookId.eq(query.getAccountBookId()))
                .and(qSalesOrder.orderStatus.eq(OrderStatus.已审核));
        applyLocalDateRange(builder, qSalesOrder.orderDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qSalesOrder)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qSalesOrder.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qSalesOrder)
                .select(qSalesOrder, qCustomer.name, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOrder.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesOrder.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qSalesOrder.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qSalesOrder.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            SalesOrder o = t.get(qSalesOrder);
            list.add(toCandidate(o.getId(), o.getOrderNo(), o.getOrderDate(), type, o.getFinalAmount(),
                    t.get(qCustomer.name), null, t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), o.getCustomerId(), null));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesSalesOutbound(Page page, CandidateQuery query) {
        String type = "销售出库单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qSalesOutbound.merchantId.eq(query.getMerchantId()))
                .and(qSalesOutbound.accountBookId.eq(query.getAccountBookId()))
                .and(qSalesOutbound.orderStatus.eq(OrderStatus.已审核));
        applyLocalDateRange(builder, qSalesOutbound.outboundDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qSalesOutbound)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qSalesOutbound.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qSalesOutbound)
                .select(qSalesOutbound, qCustomer.name, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOutbound.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesOutbound.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qSalesOutbound.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qSalesOutbound.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            SalesOutbound o = t.get(qSalesOutbound);
            list.add(toCandidate(o.getId(), o.getOrderNo(), o.getOutboundDate(), type, o.getFinalAmount(),
                    t.get(qCustomer.name), null, t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), o.getCustomerId(), null));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesSalesReturn(Page page, CandidateQuery query) {
        String type = "销售退货单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qSalesReturn.merchantId.eq(query.getMerchantId()))
                .and(qSalesReturn.accountBookId.eq(query.getAccountBookId()))
                .and(qSalesReturn.orderStatus.eq(OrderStatus.已审核));
        applyLocalDateRange(builder, qSalesReturn.returnDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qSalesReturn)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qSalesReturn.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qSalesReturn)
                .select(qSalesReturn, qCustomer.name, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesReturn.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesReturn.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qSalesReturn.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qSalesReturn.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            SalesReturn o = t.get(qSalesReturn);
            list.add(toCandidate(o.getId(), o.getOrderNo(), o.getReturnDate(), type, o.getFinalAmount(),
                    t.get(qCustomer.name), null, t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), o.getCustomerId(), null));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesInventoryTransfer(Page page, CandidateQuery query) {
        String type = "调拨单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qInventoryTransfer.merchantId.eq(query.getMerchantId()))
                .and(qInventoryTransfer.accountBookId.eq(query.getAccountBookId()))
                .and(qInventoryTransfer.orderStatus.eq(OrderStatus.已审核));
        applyUtilDateRange(builder, qInventoryTransfer.transferDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qInventoryTransfer)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qInventoryTransfer.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qInventoryTransfer)
                .select(qInventoryTransfer, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qInventoryTransfer.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qInventoryTransfer.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qInventoryTransfer.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            InventoryTransfer o = t.get(qInventoryTransfer);
            list.add(toCandidate(o.getId(), o.getOrderNo(), toLocalDate(o.getTransferDate()), type, null,
                    null, null, t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), null, null));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesStockTake(Page page, CandidateQuery query) {
        String type = "盘点单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qStockTake.merchantId.eq(query.getMerchantId()))
                .and(qStockTake.accountBookId.eq(query.getAccountBookId()))
                .and(qStockTake.orderStatus.eq(OrderStatus.已审核));
        applyUtilDateRange(builder, qStockTake.checkDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qStockTake)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qStockTake.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qStockTake)
                .select(qStockTake, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qStockTake.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qStockTake.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qStockTake.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            StockTake o = t.get(qStockTake);
            list.add(toCandidate(o.getId(), o.getOrderNo(), toLocalDate(o.getCheckDate()), type, null,
                    null, null, t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), null, null));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesOtherInbound(Page page, CandidateQuery query) {
        String type = "其他入库单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qOtherInbound.merchantId.eq(query.getMerchantId()))
                .and(qOtherInbound.accountBookId.eq(query.getAccountBookId()))
                .and(qOtherInbound.orderStatus.eq(OrderStatus.已审核));
        applyUtilDateRange(builder, qOtherInbound.inboundDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qOtherInbound)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qOtherInbound.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qOtherInbound)
                .select(qOtherInbound, qCustomer.name, qSupplier.name, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qOtherInbound.customerId))
                .leftJoin(qSupplier).on(qSupplier.id.eq(qOtherInbound.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qOtherInbound.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qOtherInbound.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qOtherInbound.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            OtherInbound o = t.get(qOtherInbound);
            list.add(toCandidate(o.getId(), o.getOrderNo(), toLocalDate(o.getInboundDate()), type, o.getFinalAmount(),
                    t.get(qCustomer.name), t.get(qSupplier.name), t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), o.getCustomerId(), o.getSupplierId()));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesOtherOutbound(Page page, CandidateQuery query) {
        String type = "其他出库单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qOtherOutbound.merchantId.eq(query.getMerchantId()))
                .and(qOtherOutbound.accountBookId.eq(query.getAccountBookId()))
                .and(qOtherOutbound.orderStatus.eq(OrderStatus.已审核));
        applyUtilDateRange(builder, qOtherOutbound.inboundDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qOtherOutbound)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qOtherOutbound.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qOtherOutbound)
                .select(qOtherOutbound, qCustomer.name, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qOtherOutbound.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qOtherOutbound.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qOtherOutbound.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qOtherOutbound.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            OtherOutbound o = t.get(qOtherOutbound);
            list.add(toCandidate(o.getId(), o.getOrderNo(), toLocalDate(o.getInboundDate()), type, o.getFinalAmount(),
                    t.get(qCustomer.name), null, t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), o.getCustomerId(), null));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesCostAdjustment(Page page, CandidateQuery query) {
        String type = "成本调整单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qCostAdjustment.merchantId.eq(query.getMerchantId()))
                .and(qCostAdjustment.accountBookId.eq(query.getAccountBookId()))
                .and(qCostAdjustment.orderStatus.eq(OrderStatus.已审核));
        applyUtilDateRange(builder, qCostAdjustment.djustmentDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qCostAdjustment)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qCostAdjustment.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qCostAdjustment)
                .select(qCostAdjustment, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qCostAdjustment.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qCostAdjustment.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qCostAdjustment.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            CostAdjustment o = t.get(qCostAdjustment);
            list.add(toCandidate(o.getId(), o.getOrderNo(), toLocalDate(o.getDjustmentDate()), type, o.getAdjustmentAmount(),
                    null, null, t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), null, null));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesOrderReceipt(Page page, CandidateQuery query) {
        String type = "收款单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qOrderReceipt.merchantId.eq(query.getMerchantId()))
                .and(qOrderReceipt.accountBookId.eq(query.getAccountBookId()))
                .and(qOrderReceipt.orderStatus.eq(OrderStatus.已审核));
        applyLocalDateRange(builder, qOrderReceipt.orderDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qOrderReceipt)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qOrderReceipt.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qOrderReceipt)
                .select(qOrderReceipt, qCustomer.name, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qOrderReceipt.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qOrderReceipt.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qOrderReceipt.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qOrderReceipt.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            OrderReceipt o = t.get(qOrderReceipt);
            String cname = t.get(qCustomer.name) != null ? t.get(qCustomer.name) : o.getCustomerName();
            list.add(toCandidate(o.getId(), o.getOrderNo(), o.getOrderDate(), type, o.getCollectionAmount(),
                    cname, null, t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), o.getCustomerId(), null));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesOrderPayment(Page page, CandidateQuery query) {
        String type = "付款单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qOrderPayment.merchantId.eq(query.getMerchantId()))
                .and(qOrderPayment.accountBookId.eq(query.getAccountBookId()))
                .and(qOrderPayment.orderStatus.eq(OrderStatus.已审核));
        applyLocalDateRange(builder, qOrderPayment.orderDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qOrderPayment)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qOrderPayment.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qOrderPayment)
                .select(qOrderPayment, qSupplier.name, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qOrderPayment.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qOrderPayment.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qOrderPayment.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qOrderPayment.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            OrderPayment o = t.get(qOrderPayment);
            String sname = t.get(qSupplier.name) != null ? t.get(qSupplier.name) : o.getSupplierName();
            list.add(toCandidate(o.getId(), o.getOrderNo(), o.getOrderDate(), type, o.getCollectionAmount(),
                    null, sname, t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), null, o.getSupplierId()));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesVerification(Page page, CandidateQuery query) {
        String type = "核销单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qVerification.merchantId.eq(query.getMerchantId()))
                .and(qVerification.accountBookId.eq(query.getAccountBookId()))
                .and(qVerification.orderStatus.eq(OrderStatus.已审核));
        if (query.getStartDate() != null) {
            builder.and(qVerification.orderDate.goe(query.getStartDate().atStartOfDay()));
        }
        if (query.getEndDate() != null) {
            builder.and(qVerification.orderDate.loe(query.getEndDate().atTime(23, 59, 59)));
        }
        applyExclude(builder, query);
        long total = bqf.selectFrom(qVerification)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qVerification.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qVerification)
                .select(qVerification, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qMerchantUser).on(qVerification.createdBy.isNotNull().and(qMerchantUser.id.eq(qVerification.createdBy.castToNum(Long.class))))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qVerification.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qVerification.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            Verification o = t.get(qVerification);
            java.math.BigDecimal amount = bqf.selectFrom(qVerificationItem)
                    .select(qVerificationItem.currentVerifyAmount.sum())
                    .where(qVerificationItem.verificationId.eq(o.getId())).fetchFirst();
            list.add(toCandidate(o.getId(), o.getOrderNo(), toLocalDate(o.getOrderDate()), type, amount,
                    null, null, t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), null, null));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesOtherReceipt(Page page, CandidateQuery query) {
        String type = "其他收款单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qOtherReceipt.merchantId.eq(query.getMerchantId()))
                .and(qOtherReceipt.accountBookId.eq(query.getAccountBookId()))
                .and(qOtherReceipt.orderStatus.eq(OrderStatus.已审核));
        applyLocalDateRange(builder, qOtherReceipt.orderDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qOtherReceipt)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qOtherReceipt.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qOtherReceipt)
                .select(qOtherReceipt, qCustomer.name, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qOtherReceipt.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qOtherReceipt.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qOtherReceipt.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qOtherReceipt.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            OtherReceipt o = t.get(qOtherReceipt);
            String cname = t.get(qCustomer.name) != null ? t.get(qCustomer.name) : o.getCustomerName();
            list.add(toCandidate(o.getId(), o.getOrderNo(), o.getOrderDate(), type, o.getCollectionAmount(),
                    cname, null, t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), o.getCustomerId(), null));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesOtherExpense(Page page, CandidateQuery query) {
        String type = "其他付款单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qOtherExpense.merchantId.eq(query.getMerchantId()))
                .and(qOtherExpense.accountBookId.eq(query.getAccountBookId()))
                .and(qOtherExpense.orderStatus.eq(OrderStatus.已审核));
        applyLocalDateRange(builder, qOtherExpense.orderDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qOtherExpense)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qOtherExpense.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qOtherExpense)
                .select(qOtherExpense, qSupplier.name, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qOtherExpense.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qOtherExpense.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qOtherExpense.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qOtherExpense.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            OtherExpense o = t.get(qOtherExpense);
            String sname = t.get(qSupplier.name) != null ? t.get(qSupplier.name) : o.getSupplierName();
            list.add(toCandidate(o.getId(), o.getOrderNo(), o.getOrderDate(), type, o.getCollectionAmount(),
                    null, sname, t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), null, o.getSupplierId()));
        }
        return new PageResults<>(list, page, total);
    }

    private PageResults<FinanceVoucherCandidateVO> candidatesAccountTransfer(Page page, CandidateQuery query) {
        String type = "转帐单";
        BooleanBuilder builder = new BooleanBuilder()
                .and(qAccountTransfer.merchantId.eq(query.getMerchantId()))
                .and(qAccountTransfer.accountBookId.eq(query.getAccountBookId()))
                .and(qAccountTransfer.orderStatus.eq(OrderStatus.已审核));
        applyLocalDateRange(builder, qAccountTransfer.orderDate, query);
        applyExclude(builder, query);
        long total = bqf.selectFrom(qAccountTransfer)
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qAccountTransfer.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).fetchCount();
        List<Tuple> rows = bqf.selectFrom(qAccountTransfer)
                .select(qAccountTransfer, qMerchantUser.name, qFinanceVoucher.id, qFinanceVoucher.code)
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qAccountTransfer.createdBy))
                .leftJoin(qFinanceVoucher).on(voucherJoinOn(qAccountTransfer.id, type, query.getMerchantId(), query.getAccountBookId()))
                .where(builder).orderBy(qAccountTransfer.id.desc())
                .offset(page.getOffset()).limit(page.getOffsetEnd()).fetch();
        List<FinanceVoucherCandidateVO> list = new ArrayList<>();
        for (Tuple t : rows) {
            AccountTransfer o = t.get(qAccountTransfer);
            list.add(toCandidate(o.getId(), o.getOrderNo(), o.getOrderDate(), type, o.getAmount(),
                    null, null, t.get(qMerchantUser.name), t.get(qFinanceVoucher.code),
                    t.get(qFinanceVoucher.id), null, null));
        }
        return new PageResults<>(list, page, total);
    }

    @Transactional
    public void batchDelete(List<Long> financeVoucherIds, Long merchantId, Long accountBookId) {
        if (financeVoucherIds == null || financeVoucherIds.isEmpty()) {
            return;
        }
        jqf.delete(qFinanceVoucher)
                .where(qFinanceVoucher.id.in(financeVoucherIds)
                        .and(qFinanceVoucher.merchantId.eq(merchantId))
                        .and(qFinanceVoucher.accountBookId.eq(accountBookId)))
                .execute();
    }

    @Transactional
    public void batchGenerate(List<FinanceVoucherForm> forms, Long merchantId, Long accountBookId) throws JsonProcessingException, UnsupportedEncodingException {
        if (forms == null || forms.isEmpty()) {
            return;
        }
        for (FinanceVoucherForm form : forms) {
            form.setMerchantId(merchantId);
            form.setAccountBookId(accountBookId);
            save(form);
        }
    }

    @Data
    public static class CandidateQuery implements TenantAware {
        private Long merchantId;
        private Long accountBookId;
        private String documentType;
        private boolean excludeVouchered = true;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class Query implements TenantAware {

        private String type;

        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qFinanceVoucher.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qFinanceVoucher.accountBookId, accountBookId);
        }

        public BooleanBuilder builders() {
            if (StrUtil.isNotBlank(type)) {
                builder.and(qFinanceVoucher.type.eq(type));
            }
            return builder;
        }
    }
}
