package com.flyemu.share.service.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fenxi365.api.Fenxi365;
import com.fenxi365.api.Result;
import com.fenxi365.api.tax.InvoiceRedServer;
import com.fenxi365.api.tax.QdfpServer;
import com.fenxi365.open.model.dto.invoice.qdfp.KptjxxcxDto;
import com.fenxi365.open.model.dto.invoice.red.HzFpkjDto;
import com.fenxi365.open.model.dto.invoice.red.HzqrxxListDto;
import com.fenxi365.open.model.dto.invoice.red.HzqrxxSaveDto;
import com.fenxi365.open.model.enums.LoginType;
import com.fenxi365.open.model.enums.ServiceType;
import com.fenxi365.open.model.dto.*;
import com.fenxi365.open.model.vo.*;
import com.fenxi365.open.model.vo.invoice.qdfp.KptjxxcxVo;
import com.fenxi365.open.model.vo.invoice.red.HzFpkjVo;
import com.fenxi365.open.model.vo.invoice.red.HzqrxxListVo;
import com.fenxi365.open.model.vo.invoice.red.HzqrxxSaveVo;
import com.flyemu.share.config.InvoicePlatformProperties;
import com.flyemu.share.dto.invoice.InvoiceRequest;
import com.flyemu.share.dto.invoice.PagedResponse;
import com.flyemu.share.dto.invoice.RedInvoiceResponse;
import com.flyemu.share.dto.invoice.StatsResponse;
import com.flyemu.share.entity.invoice.Invoice;
import com.flyemu.share.entity.invoice.InvoiceItem;
import com.flyemu.share.entity.invoice.InvoiceSellerConfig;
import com.flyemu.share.entity.sales.SalesOutbound;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.repository.invoice.InvoiceRepository;
import com.flyemu.share.repository.sales.SalesOutboundRepository;
import com.flyemu.share.service.sales.SalesOutboundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SalesOutboundRepository salesOutboundRepository;
    private final SalesOutboundService salesOutboundService;
    private final AuthService authService;
    private final InvoiceSellerConfigService sellerConfigService;
    private final Fenxi365 fenxi365;
    private final InvoicePlatformProperties props;
    private static final ObjectMapper om = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private QdfpServer qdfp() {
        return fenxi365.withLoginTypeAndService(LoginType.TG, ServiceType.QXY_SDKP).getQdfpServer();
    }

    private InvoiceRedServer red() {
        return fenxi365.withLoginTypeAndService(LoginType.TG, ServiceType.QXY_SDKP).getInvoiceRedServer();
    }

    /**
     * 销方信息：优先使用商户在「税号配置」里保存的销方信息，未配置的字段回退到平台默认配置。
     */
    private InvoicePlatformProperties.Seller effectiveSeller(Long merchantId) {
        InvoiceSellerConfig cfg = sellerConfigService.get(merchantId);
        InvoicePlatformProperties.Seller seller = new InvoicePlatformProperties.Seller();
        seller.setName(props.getSeller().getName());
        seller.setPhone(props.getSeller().getPhone());
        seller.setBank(props.getSeller().getBank());
        seller.setBankAccount(props.getSeller().getBankAccount());
        seller.setInvoiceType(props.getSeller().getInvoiceType());
        if (cfg != null) {
            if (cfg.getName() != null && !cfg.getName().isEmpty()) seller.setName(cfg.getName());
            if (cfg.getPhone() != null && !cfg.getPhone().isEmpty()) seller.setPhone(cfg.getPhone());
            if (cfg.getBank() != null && !cfg.getBank().isEmpty()) seller.setBank(cfg.getBank());
            if (cfg.getBankAccount() != null && !cfg.getBankAccount().isEmpty()) seller.setBankAccount(cfg.getBankAccount());
            if (cfg.getInvoiceType() != null && !cfg.getInvoiceType().isEmpty()) seller.setInvoiceType(cfg.getInvoiceType());
        }
        return seller;
    }

    /**
     * 销方税号：优先使用商户保存的销方税号，未配置则回退到登录账号税号。
     */
    private String effectiveNsrsbh(Long merchantId) {
        InvoiceSellerConfig cfg = sellerConfigService.get(merchantId);
        if (cfg != null && cfg.getNsrsbh() != null && !cfg.getNsrsbh().isEmpty()) {
            return cfg.getNsrsbh();
        }
        return authService.getNsrsbh(merchantId);
    }

    // ========== 品名查询 ==========

    public Map<String, Object> searchGoodsTax(Long merchantId, String goodsName) {
        if (props.isMockEnabled()) {
            Map<String, Object> r = new HashMap<>();
            r.put("goodsCode", "3040801");
            r.put("taxRate", 0.06);
            return r;
        }
        SpxxZnFmVo vo = new SpxxZnFmVo();
        vo.setAccountId(authService.getAccountId(merchantId));
        vo.setAggOrgId(authService.getAggOrgId(merchantId));
        vo.setNsrsbh(authService.getNsrsbh(merchantId));
        SpxxZnFmVo.Data data = new SpxxZnFmVo.Data();
        data.setXmmc(goodsName);
        vo.setData(data);
        Result<SpxxZnFmDto> result = qdfp().spxxZnFm(vo);
        if (!result.isSuccess()) throw new RuntimeException("品名查询失败: " + result.getMsg());
        Map<String, Object> r = new HashMap<>();
        SpxxZnFmDto dto = result.getData();
        if (dto.getList() != null && !dto.getList().isEmpty()) {
            var first = dto.getList().get(0);
            String code = first.getSphfwssflhbbm();
            if (code == null || code.isEmpty()) {
                code = first.getSpsjbm();
            }
            r.put("goodsCode", code);
            r.put("goodsShortName", first.getSpfwjc());
            log.info("品名查询: 输入={}, spsjbm={}, sphfwssflhbbm={}, spfwjc={}, 最终code={}",
                    goodsName, first.getSpsjbm(), first.getSphfwssflhbbm(), first.getSpfwjc(), code);
            String[] slvList = first.getSlvList();
            if (slvList != null && slvList.length > 0)
                r.put("taxRate", Double.parseDouble(slvList[0]));
        }
        return r;
    }

    // ========== 蓝字发票 ==========

    @Transactional
    public Invoice issueInvoice(Long merchantId, Long accountBookId, InvoiceRequest request) {
        if (!authService.isLoggedIn(merchantId))
            throw new RuntimeException("请先完成登录认证");

        String invoiceNo = "INV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Invoice invoice = new Invoice();
        invoice.setInvoiceType("BLUE");
        invoice.setInvoiceNo(invoiceNo);
        invoice.setBuyerName(request.getBuyerName());
        invoice.setBuyerTaxNo(request.getBuyerTaxNo());
        invoice.setStatus("PROCESSING");
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setMerchantId(merchantId);
        invoice.setAccountBookId(accountBookId);
        invoice.setSourceId(request.getSourceId());
        invoice.setSourceType(request.getSourceType());

        List<InvoiceItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        int lineNo = 1;

        for (var ir : request.getItems()) {
            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setLineNo(lineNo++);
            item.setGoodsName(ir.getGoodsName());
            item.setQuantity(ir.getQuantity());
            item.setUnitPrice(ir.getUnitPrice());
            item.setTaxRate(ir.getTaxRate());
            item.setMerchantId(merchantId);
            item.setAccountBookId(accountBookId);

            try {
                Map<String, Object> taxInfo = searchGoodsTax(merchantId, ir.getGoodsName());
                item.setGoodsCode((String) taxInfo.get("goodsCode"));
                item.setGoodsShortName((String) taxInfo.get("goodsShortName"));
            } catch (Exception e) {
                log.warn("品名查询失败: {}", ir.getGoodsName());
            }

            BigDecimal lineTotal = ir.getQuantity().multiply(ir.getUnitPrice());
            BigDecimal taxExclusive = lineTotal.divide(BigDecimal.ONE.add(ir.getTaxRate()), 2, RoundingMode.HALF_UP);
            BigDecimal tax = taxExclusive.multiply(ir.getTaxRate()).setScale(2, RoundingMode.HALF_UP);
            item.setAmount(taxExclusive);
            item.setTax(tax);
            items.add(item);
            totalAmount = totalAmount.add(lineTotal);
            totalTax = totalTax.add(tax);
        }

        invoice.setItems(items);
        invoice.setTotalAmount(totalAmount);
        invoice.setTotalTax(totalTax);

        if (props.isMockEnabled()) {
            log.info("[MOCK] 蓝字发票开具成功");
            invoice.setThirdPartyCode("MOCK_CODE_" + System.currentTimeMillis());
            invoice.setThirdPartyNumber("MOCK_NO_" + System.currentTimeMillis());
            invoice.setPdfUrl("http://mock.pdf/" + invoiceNo + ".pdf");
            invoice.setStatus("ISSUED");
            Invoice saved = invoiceRepository.save(invoice);
            writeBackToSource(saved);
            return saved;
        }

        var seller = effectiveSeller(merchantId);
        String nsrsbh = effectiveNsrsbh(merchantId);

        FpkjZzsVo vo = new FpkjZzsVo();
        vo.setAccountId(authService.getAccountId(merchantId));
        vo.setAggOrgId(authService.getAggOrgId(merchantId));
        vo.setNsrsbh(nsrsbh);

        FpkjZzsVo.Data data = new FpkjZzsVo.Data();
        data.setGmf_nsrsbh(invoice.getBuyerTaxNo());
        data.setGmf_nsrmc(invoice.getBuyerName());
        data.setGmf_lx(invoice.getBuyerTaxNo() != null && !invoice.getBuyerTaxNo().isEmpty() ? "0" : "1");
        data.setXsf_nsrsbh(nsrsbh);
        data.setXsf_nsrmc(seller.getName());
        data.setXsf_dh(seller.getPhone());
        data.setXsf_yhmc(seller.getBank());
        data.setXsf_yhzh(seller.getBankAccount());
        data.setFplxdm(seller.getInvoiceType());
        data.setLy_ddbh(invoiceNo);
        data.setWjgs("PDF,OFD,XML");
        if (invoice.getTotalAmount() != null) data.setJshj(invoice.getTotalAmount().toPlainString());
        if (request.getRemark() != null && !request.getRemark().isBlank())
            data.setBz(request.getRemark());
        if (request.getPayee() != null && !request.getPayee().isBlank())
            data.setSkr(request.getPayee());
        if (request.getReviewer() != null && !request.getReviewer().isBlank())
            data.setFhr(request.getReviewer());

        List<FpkjZzsVo.Xmmx> xmmxList = new ArrayList<>();
        for (InvoiceItem item : items) {
            FpkjZzsVo.Xmmx xmmx = new FpkjZzsVo.Xmmx();
            xmmx.setXmmc(item.getGoodsName());
            xmmx.setFphxz("0");
            xmmx.setSpbm(item.getGoodsCode() != null ? item.getGoodsCode() : "3040801");
            xmmx.setSpmc(item.getGoodsShortName() != null ? item.getGoodsShortName() : item.getGoodsName());
            if (item.getTaxRate() != null) xmmx.setSl(item.getTaxRate().toPlainString());
            if (item.getQuantity() != null) xmmx.setSpsl(item.getQuantity().toPlainString());
            BigDecimal je = item.getQuantity() != null && item.getUnitPrice() != null
                    ? item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP)
                    : null;
            if (je != null) xmmx.setJe(je.toPlainString());
            xmmxList.add(xmmx);
        }
        data.setXmmx(xmmxList);
        vo.setData(data);

        try {
            log.info("开票请求JSON:\n{}", om.writeValueAsString(vo));
        } catch (Exception ignored) {}

        log.info("开票请求: nsrsbh={}, 销方={}, 购方={}/{}, 明细数={}, 金额={}, 税额={}",
                nsrsbh, seller.getName(), invoice.getBuyerName(), invoice.getBuyerTaxNo(),
                xmmxList.size(),
                invoice.getTotalAmount() != null ? invoice.getTotalAmount().toPlainString() : "0",
                invoice.getTotalTax() != null ? invoice.getTotalTax().toPlainString() : "0");

        Result<FpkjZzsDto> result = qdfp().fpkjZzs(vo);
        if (!result.isSuccess()) throw new RuntimeException("开票失败: " + result.getMsg());
        FpkjZzsDto dto = result.getData();

        invoice.setThirdPartyCode(dto.getFpdm());
        invoice.setThirdPartyNumber(dto.getFphm());
        invoice.setPdfUrl(dto.getPdf_url());
        invoice.setStatus("ISSUED");
        log.info("蓝字发票开具成功: 发票代码={}, 发票号码={}, PDF={}", dto.getFpdm(), dto.getFphm(), dto.getPdf_url());
        invoice = invoiceRepository.save(invoice);
        writeBackToSource(invoice);
        fetchAndStorePdf(merchantId, invoice, dto.getFphm());
        return invoice;
    }

    /**
     * 批量开票：对选中的已审核且未开票的销售出库单逐一开具蓝字发票。
     * 返回成功张数与逐单错误信息（先用后优：单张失败不影响其余）。
     */
    @Transactional
    public Map<String, Object> batchIssue(Long merchantId, Long accountBookId, List<Long> salesOutboundIds) {
        int success = 0;
        List<String> errors = new ArrayList<>();
        for (Long id : salesOutboundIds) {
            SalesOutbound so = salesOutboundRepository.findById(id)
                    .filter(s -> merchantId.equals(s.getMerchantId()))
                    .orElse(null);
            try {
                if (so == null) {
                    errors.add("单据#" + id + "：不存在");
                    continue;
                }
                if (so.getOrderStatus() != OrderStatus.已审核) {
                    errors.add((so.getOrderNo() == null ? "单据#" + id : so.getOrderNo()) + "：未审核，无法开票");
                    continue;
                }
                if ("已开票".equals(so.getInvoiceStatus())) {
                    errors.add((so.getOrderNo() == null ? "单据#" + id : so.getOrderNo()) + "：已开票");
                    continue;
                }
                issueInvoice(merchantId, accountBookId, buildInvoiceRequest(merchantId, so));
                success++;
            } catch (Exception e) {
                errors.add((so != null && so.getOrderNo() != null ? so.getOrderNo() : "单据#" + id) + "：" + e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("errors", errors);
        result.put("message", "成功开票 " + success + " 张" + (errors.isEmpty() ? "" : "，失败 " + errors.size() + " 张"));
        return result;
    }

    private InvoiceRequest buildInvoiceRequest(Long merchantId, SalesOutbound so) {
        Map<String, Object> pre = salesOutboundService.prefillInvoice(merchantId, so.getId());
        InvoiceRequest req = new InvoiceRequest();
        req.setBuyerName((String) pre.get("buyerName"));
        req.setBuyerTaxNo((String) pre.get("buyerTaxNo"));
        req.setSourceId(so.getId());
        req.setSourceType("SALES_OUTBOUND");
        List<InvoiceRequest.ItemRequest> items = new ArrayList<>();
        Object rawItems = pre.get("items");
        if (rawItems instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> m)) {
                    continue;
                }
                InvoiceRequest.ItemRequest ir = new InvoiceRequest.ItemRequest();
                ir.setGoodsName(m.get("goodsName") == null ? "" : m.get("goodsName").toString());
                ir.setQuantity(toBd(m.get("quantity")));
                ir.setUnitPrice(toBd(m.get("unitPrice")));
                ir.setTaxRate(m.get("taxRate") == null ? new BigDecimal("0.06") : toBd(m.get("taxRate")));
                items.add(ir);
            }
        }
        req.setItems(items);
        return req;
    }

    private BigDecimal toBd(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    // 开票成功后，把发票ID与开票状态回写到来源单据（当前仅支持销售出库单）
    private void writeBackToSource(Invoice invoice) {
        if (invoice.getSourceId() == null || !"SALES_OUTBOUND".equals(invoice.getSourceType())) return;
        if (!"ISSUED".equals(invoice.getStatus())) return;
        salesOutboundRepository.findById(invoice.getSourceId())
                .filter(so -> invoice.getMerchantId().equals(so.getMerchantId()))
                .ifPresent(so -> {
                    so.setInvoiceId(invoice.getId());
                    so.setInvoiceStatus("已开票");
                    salesOutboundRepository.save(so);
                });
    }

    // ========== PDF下载 ==========

    public byte[] downloadPdf(Long merchantId, Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .filter(i -> merchantId.equals(i.getMerchantId()))
                .orElseThrow(() -> new RuntimeException("发票不存在: id=" + id));

        if (props.isMockEnabled())
            return ("Mock PDF Content for invoice " + id).getBytes();

        if (invoice.getPdfUrl() != null && !invoice.getPdfUrl().isEmpty()) {
            java.io.File f = new java.io.File(invoice.getPdfUrl());
            if (f.exists()) {
                try { return java.nio.file.Files.readAllBytes(f.toPath()); }
                catch (Exception e) { log.warn("本地PDF读取失败: {}", e.getMessage()); }
            }
            if (invoice.getPdfUrl().startsWith("http")) {
                try {
                    log.info("OSS下载PDF: {}", invoice.getPdfUrl());
                    return java.net.URI.create(invoice.getPdfUrl()).toURL().openStream().readAllBytes();
                } catch (Exception e) {
                    log.warn("OSS下载失败，尝试API: {}", e.getMessage());
                }
            }
        }

        if (invoice.getThirdPartyNumber() == null)
            throw new RuntimeException("发票无第三方号码");

        return downloadByFphm(merchantId, invoice.getThirdPartyNumber(), invoice.getIssueDate());
    }

    private byte[] downloadByFphm(Long merchantId, String fphm, LocalDateTime issueDate) {
        String kprq = issueDate != null
                ? issueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        com.fenxi365.open.model.vo.invoice.sdfile.SyncLayoutFileVo vo =
                new com.fenxi365.open.model.vo.invoice.sdfile.SyncLayoutFileVo();
        vo.setAccountId(authService.getAccountId(merchantId));
        vo.setAggOrgId(authService.getAggOrgId(merchantId));
        vo.setNsrsbh(authService.getNsrsbh(merchantId));
        vo.setFphm(fphm);
        vo.setKprq(kprq);
        vo.setJxxbz("2");

        com.fenxi365.api.tax.SdFileServer server = fenxi365
                .withLoginTypeAndService(LoginType.TG, ServiceType.QXY_SDKP)
                .getSdFileServer();
        var result = server.syncLayoutFile(vo);
        if (!result.isSuccess())
            throw new RuntimeException("下载PDF失败: " + result.getMsg());
        String pdfUrl = result.getData().getPdfFile();
        if (pdfUrl != null && !pdfUrl.isEmpty()) {
            try { return java.net.URI.create(pdfUrl).toURL().openStream().readAllBytes(); }
            catch (Exception e) { throw new RuntimeException("PDF URL下载失败: " + e.getMessage()); }
        }
        String ofdUrl = result.getData().getOfdFile();
        if (ofdUrl != null && !ofdUrl.isEmpty()) {
            try { return java.net.URI.create(ofdUrl).toURL().openStream().readAllBytes(); }
            catch (Exception e) { throw new RuntimeException("OFD URL下载失败: " + e.getMessage()); }
        }
        throw new RuntimeException("PDF/OFD URL为空");
    }

    private void fetchAndStorePdf(Long merchantId, Invoice invoice, String fphm) {
        String path = "data/pdf/" + fphm + ".pdf";
        for (int i = 0; i < 5; i++) {
            try {
                byte[] pdf = downloadByFphm(merchantId, fphm, invoice.getIssueDate());
                new java.io.File("data/pdf").mkdirs();
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(path)) { fos.write(pdf); }
                invoice.setPdfUrl(path);
                invoiceRepository.save(invoice);
                log.info("PDF已保存: fphm={}", fphm);
                return;
            } catch (Exception e) {
                log.info("PDF重试{}/5: {}", i + 1, e.getMessage());
                try { Thread.sleep(3000L * (i + 1)); } catch (InterruptedException ie) { break; }
            }
        }
        log.warn("PDF最终未获取: fphm={}", fphm);
    }

    // ========== 红字发票 ==========

    public RedInvoiceResponse applyRedInvoice(Long merchantId, String originalInvoiceCode, String originalInvoiceNo,
                                              String originalInvoiceDate, String redReason) {
        if (props.isMockEnabled()) {
            RedInvoiceResponse r = new RedInvoiceResponse();
            r.setRedInfoNo("MOCK_RED_INFO_" + System.currentTimeMillis());
            r.setStatus("APPLIED");
            return r;
        }
        HzqrxxSaveVo vo = new HzqrxxSaveVo();
        vo.setAccountId(authService.getAccountId(merchantId));
        vo.setAggOrgId(authService.getAggOrgId(merchantId));
        vo.setNsrsbh(authService.getNsrsbh(merchantId));
        HzqrxxSaveVo.Data data = new HzqrxxSaveVo.Data();
        data.setLzFphm(originalInvoiceNo);
        data.setLzKprq(normalizeDate(originalInvoiceDate));
        HzqrxxSaveVo.Hcyydm hcyy;
        try {
            hcyy = HzqrxxSaveVo.Hcyydm.forValue(redReason != null ? redReason : "01");
        } catch (Exception e) {
            hcyy = HzqrxxSaveVo.Hcyydm.THE_01;
        }
        data.setHcyydm(hcyy);
        data.setWjgs("PDF,OFD,XML");
        data.setXsfNsrsbh(authService.getNsrsbh(merchantId));
        vo.setData(data);
        Result<HzqrxxSaveDto> result = red().hzqrxxSave(vo);
        if (!result.isSuccess()) throw new RuntimeException("红字申请失败: " + result.getMsg());
        HzqrxxSaveDto dto = result.getData();
        RedInvoiceResponse r = new RedInvoiceResponse();
        r.setRedInfoNo(dto.getHzuuid());
        r.setStatus(dto.getHzqrztDm());
        return r;
    }

    @Transactional
    public RedInvoiceResponse issueRedInvoice(Long merchantId, Long accountBookId, String redInfoNo,
                                              String originalInvoiceNo, String originalInvoiceDate) {
        Invoice original = null;
        if (originalInvoiceNo != null && !originalInvoiceNo.isEmpty()) {
            original = invoiceRepository.findByMerchantIdAndThirdPartyNumber(merchantId, originalInvoiceNo);
        }

        if (props.isMockEnabled()) {
            Invoice redInv = new Invoice();
            redInv.setInvoiceType("RED");
            redInv.setInvoiceNo("RED" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            redInv.setOriginalInvoiceNo(originalInvoiceNo);
            redInv.setOriginalInvoiceDate(originalInvoiceDate);
            redInv.setBuyerName(original != null ? original.getBuyerName() : "模拟购买方");
            redInv.setBuyerTaxNo(original != null ? original.getBuyerTaxNo() : "");
            redInv.setTotalAmount(original != null ? original.getTotalAmount().negate() : BigDecimal.ZERO);
            redInv.setStatus("ISSUED");
            redInv.setIssueDate(LocalDateTime.now());
            redInv.setThirdPartyCode("MOCK_RED_CODE_" + System.currentTimeMillis());
            redInv.setThirdPartyNumber("MOCK_RED_NO_" + System.currentTimeMillis());
            redInv.setPdfUrl("http://mock.pdf/red_" + redInfoNo + ".pdf");
            redInv.setMerchantId(merchantId);
            redInv.setAccountBookId(accountBookId);
            invoiceRepository.save(redInv);

            RedInvoiceResponse r = new RedInvoiceResponse();
            r.setRedInfoNo(redInfoNo);
            r.setInvoiceCode(redInv.getThirdPartyCode());
            r.setInvoiceNumber(redInv.getThirdPartyNumber());
            r.setPdfId(redInv.getPdfUrl());
            r.setStatus("ISSUED");
            r.setId(redInv.getId());
            return r;
        }
        HzFpkjVo vo = new HzFpkjVo();
        vo.setAccountId(authService.getAccountId(merchantId));
        vo.setAggOrgId(authService.getAggOrgId(merchantId));
        vo.setNsrsbh(authService.getNsrsbh(merchantId));
        HzFpkjVo.Data data = new HzFpkjVo.Data();
        data.setHzuuid(redInfoNo);
        data.setXsfNsrsbh(authService.getNsrsbh(merchantId));
        data.setWjgs("PDF,OFD,XML");
        vo.setData(data);
        Result<HzFpkjDto> result = red().hzFpkj(vo);
        if (!result.isSuccess()) throw new RuntimeException("红字开票失败: " + result.getMsg());
        HzFpkjDto dto = result.getData();

        Invoice redInv = new Invoice();
        redInv.setInvoiceType("RED");
        redInv.setInvoiceNo("RED" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        redInv.setOriginalInvoiceNo(originalInvoiceNo);
        redInv.setOriginalInvoiceDate(originalInvoiceDate);
        redInv.setBuyerName(original != null ? original.getBuyerName() : "");
        redInv.setBuyerTaxNo(original != null ? original.getBuyerTaxNo() : "");
        redInv.setTotalAmount(original != null ? original.getTotalAmount().negate() : BigDecimal.ZERO);
        redInv.setStatus("ISSUED");
        redInv.setIssueDate(LocalDateTime.now());
        redInv.setThirdPartyCode(dto.getFpdm());
        redInv.setThirdPartyNumber(dto.getFphm());
        String ossUrl = dto.getPdfurl();
        if (ossUrl == null || ossUrl.isEmpty()) ossUrl = dto.getOfdurl();
        if (ossUrl == null || ossUrl.isEmpty()) ossUrl = dto.getXmlurl();
        redInv.setPdfUrl(ossUrl);
        redInv.setMerchantId(merchantId);
        redInv.setAccountBookId(accountBookId);
        log.info("红字发票开具: pdf={}, ofd={}, xml={}, ewm={}, fphm={}",
                dto.getPdfurl(), dto.getOfdurl(), dto.getXmlurl(), dto.getEwmurl(), dto.getFphm());
        invoiceRepository.save(redInv);
        fetchAndStorePdf(merchantId, redInv, dto.getFphm());

        RedInvoiceResponse r = new RedInvoiceResponse();
        r.setRedInfoNo(redInfoNo);
        r.setInvoiceCode(dto.getFpdm());
        r.setInvoiceNumber(dto.getFphm());
        r.setPdfId(dto.getPdfurl());
        r.setStatus("ISSUED");
        r.setId(redInv.getId());
        log.info("红字发票开具成功: 发票代码={}, 发票号码={}, PDF={}, 原蓝票={}",
                dto.getFpdm(), dto.getFphm(), dto.getPdfurl(), originalInvoiceNo);
        return r;
    }

    // ========== 查询 ==========

    public Invoice queryInvoice(Long merchantId, Long id) {
        return invoiceRepository.findById(id)
                .filter(i -> merchantId.equals(i.getMerchantId()))
                .orElseThrow(() -> new RuntimeException("发票不存在: id=" + id));
    }

    public List<Invoice> listInvoices(Long merchantId) {
        return invoiceRepository.findAllByMerchantIdOrderByIssueDateDesc(merchantId);
    }

    public List<Invoice> listRedInvoices(Long merchantId) {
        return invoiceRepository.findByMerchantIdAndInvoiceTypeOrderByIssueDateDesc(merchantId, "RED");
    }

    // ========== 分页查询 ==========

    public Page<Invoice> listInvoicesPaged(Long merchantId, int page, int size) {
        return invoiceRepository.findAllByMerchantIdOrderByIssueDateDesc(merchantId, PageRequest.of(page, size));
    }

    public Page<Invoice> listBlueInvoicesPaged(Long merchantId, int page, int size) {
        return invoiceRepository.findBlueInvoices(merchantId, PageRequest.of(page, size));
    }

    public Page<Invoice> listRedInvoicesPaged(Long merchantId, int page, int size) {
        return invoiceRepository.findByMerchantIdAndInvoiceTypeOrderByIssueDateDesc(merchantId, "RED", PageRequest.of(page, size));
    }

    public Page<Invoice> searchInvoices(Long merchantId, String buyerName, String status, String invoiceType, int page, int size) {
        return invoiceRepository.searchInvoices(merchantId, buyerName, status, invoiceType, PageRequest.of(page, size));
    }

    @Transactional
    public Map<String, Object> syncRedInvoices(Long merchantId, Long accountBookId) {
        if (props.isMockEnabled()) {
            return Map.of("synced", 0, "total", 0, "message", "模拟模式不支持同步");
        }

        HzqrxxListVo vo = new HzqrxxListVo();
        vo.setAccountId(authService.getAccountId(merchantId));
        vo.setAggOrgId(authService.getAggOrgId(merchantId));
        vo.setNsrsbh(authService.getNsrsbh(merchantId));
        vo.setAction(HzqrxxListVo.Action.HZQRXX_LIST);

        HzqrxxListVo.Data data = new HzqrxxListVo.Data();
        data.setPageIndex(1);
        data.setPageSize(50);

        HzqrxxListVo.Params params = new HzqrxxListVo.Params();
        params.setXsfNsrsbh(authService.getNsrsbh(merchantId));
        params.setKprqq("2026-08-10");
        params.setKprqz(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.setParams(params);
        vo.setData(data);

        Result<HzqrxxListDto> result = red().hzqrxxList(vo);
        if (!result.isSuccess()) throw new RuntimeException("查询红字发票列表失败: " + result.getMsg());

        HzqrxxListDto dto = result.getData();
        int synced = 0;
        if (dto.getList() != null) {
            for (HzqrxxListDto.RowItem row : dto.getList()) {
                if (row.getHzFphm() != null && !row.getHzFphm().isEmpty()) {
                    Invoice existing = invoiceRepository.findByMerchantIdAndThirdPartyNumber(merchantId, row.getHzFphm());
                    if (existing != null) continue;
                }
                if (row.getHzFphm() == null || row.getHzFphm().isEmpty()) continue;

                Invoice redInv = new Invoice();
                redInv.setInvoiceType("RED");
                redInv.setInvoiceNo("RED-SYNC-" + (row.getHzuuid() != null ? row.getHzuuid().substring(0, 8) : System.currentTimeMillis()));
                redInv.setOriginalInvoiceNo(row.getLzFphm());
                redInv.setOriginalInvoiceDate(row.getLzKprq());
                redInv.setBuyerName(row.getGmfNsrmc());
                redInv.setBuyerTaxNo(row.getGmfNsrsbh());
                redInv.setTotalAmount(row.getHzHjje() != null ? BigDecimal.valueOf(row.getHzHjje()) : BigDecimal.ZERO);
                redInv.setTotalTax(row.getHzHjse() != null ? BigDecimal.valueOf(row.getHzHjse()) : BigDecimal.ZERO);
                redInv.setStatus("ISSUED");
                redInv.setIssueDate(row.getHzKprq() != null && !row.getHzKprq().isEmpty()
                        ? LocalDateTime.parse(row.getHzKprq(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : LocalDateTime.now());
                redInv.setThirdPartyCode(row.getHzFpdm());
                redInv.setThirdPartyNumber(row.getHzFphm());
                redInv.setMerchantId(merchantId);
                redInv.setAccountBookId(accountBookId);
                invoiceRepository.save(redInv);
                if (row.getHzFphm() != null && !row.getHzFphm().isEmpty()) {
                    fetchAndStorePdf(merchantId, redInv, row.getHzFphm());
                }
                synced++;
                log.info("同步红字发票: 红票号={}, 原蓝票={}, 购方={}, 金额={}",
                        row.getHzFphm(), row.getLzFphm(), row.getGmfNsrmc(), row.getHzHjje());
            }
        }
        return Map.of("synced", synced, "total", dto.getRecords() != null ? dto.getRecords() : 0,
                "message", "成功同步 " + synced + " 条红字发票");
    }

    @Transactional
    public Invoice manualRedInvoice(Long merchantId, Long accountBookId, String redInvoiceNo, String redInvoiceCode, String originalInvoiceNo) {
        Invoice original = invoiceRepository.findByMerchantIdAndThirdPartyNumber(merchantId, originalInvoiceNo);
        Invoice redInv = new Invoice();
        redInv.setInvoiceType("RED");
        redInv.setInvoiceNo("RED-MANUAL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        redInv.setOriginalInvoiceNo(originalInvoiceNo);
        redInv.setOriginalInvoiceDate(original != null && original.getIssueDate() != null
                ? original.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        redInv.setBuyerName(original != null ? original.getBuyerName() : "");
        redInv.setBuyerTaxNo(original != null ? original.getBuyerTaxNo() : "");
        redInv.setTotalAmount(original != null ? original.getTotalAmount().negate() : BigDecimal.ZERO);
        redInv.setStatus("ISSUED");
        redInv.setIssueDate(LocalDateTime.now());
        redInv.setThirdPartyCode(redInvoiceCode);
        redInv.setThirdPartyNumber(redInvoiceNo);
        redInv.setPdfUrl("");
        redInv.setMerchantId(merchantId);
        redInv.setAccountBookId(accountBookId);
        invoiceRepository.save(redInv);
        log.info("手动补录红字发票: 红票号={}, 原蓝票={}, id={}", redInvoiceNo, originalInvoiceNo, redInv.getId());
        return redInv;
    }

    public Map<String, String> lookupByInvoiceNo(Long merchantId, String invoiceNo) {
        Invoice inv = invoiceRepository.findByMerchantIdAndThirdPartyNumber(merchantId, invoiceNo);
        if (inv == null) throw new RuntimeException("未找到该发票: " + invoiceNo);
        Map<String, String> r = new HashMap<>();
        r.put("invoiceNo", inv.getThirdPartyNumber());
        r.put("issueDate", inv.getIssueDate() != null
                ? inv.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        r.put("buyerName", inv.getBuyerName());
        r.put("totalAmount", inv.getTotalAmount() != null ? inv.getTotalAmount().toPlainString() : "");
        return r;
    }

    // ========== 统计 ==========

    /**
     * 销项归集：按开票月份汇总本商户开具的发票（蓝字正数、红字负数）。
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> outputAggregation(Long merchantId) {
        List<Invoice> list = invoiceRepository.findAllByMerchantIdOrderByIssueDateDesc(merchantId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, BigDecimal[]> byMonth = new TreeMap<>();
        for (Invoice inv : list) {
            String month = inv.getIssueDate() != null ? inv.getIssueDate().format(fmt) : "未登记";
            BigDecimal[] agg = byMonth.computeIfAbsent(month, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            boolean red = "RED".equals(inv.getInvoiceType());
            BigDecimal total = nz(inv.getTotalAmount());
            BigDecimal tax = nz(inv.getTotalTax());
            BigDecimal amount = total.subtract(tax);
            if (red) {
                agg[0] = agg[0].add(BigDecimal.ZERO);       // 蓝字张数
                agg[1] = agg[1].add(BigDecimal.ONE);        // 红字张数
                agg[2] = agg[2].subtract(amount);           // 不含税金额（红冲冲减）
                agg[3] = agg[3].subtract(tax);              // 税额（红冲冲减）
            } else {
                agg[0] = agg[0].add(BigDecimal.ONE);
                agg[2] = agg[2].add(amount);
                agg[3] = agg[3].add(tax);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : byMonth.entrySet()) {
            BigDecimal[] agg = e.getValue();
            Map<String, Object> m = new HashMap<>();
            m.put("month", e.getKey());
            m.put("blueCount", agg[0].intValue());
            m.put("redCount", agg[1].intValue());
            m.put("amount", agg[2].setScale(2, RoundingMode.HALF_UP));
            m.put("tax", agg[3].setScale(2, RoundingMode.HALF_UP));
            m.put("totalAmount", agg[2].add(agg[3]).setScale(2, RoundingMode.HALF_UP));
            result.add(m);
        }
        return result;
    }

    public StatsResponse getStats(Long merchantId) {
        if (props.isMockEnabled()) {
            StatsResponse r = new StatsResponse();
            r.setTotalQuota(new BigDecimal("1000000.00"));
            r.setUsedQuota(new BigDecimal("250000.00"));
            r.setRemainingQuota(new BigDecimal("750000.00"));
            r.setInvoiceCount(15);
            return r;
        }
        KptjxxcxVo vo = new KptjxxcxVo();
        vo.setAccountId(authService.getAccountId(merchantId));
        vo.setAggOrgId(authService.getAggOrgId(merchantId));
        vo.setNsrsbh(authService.getNsrsbh(merchantId));
        Result<KptjxxcxDto> result = qdfp().kptjxxcx(vo);
        if (!result.isSuccess()) throw new RuntimeException("获取统计失败: " + result.getMsg());
        KptjxxcxDto dto = result.getData();
        StatsResponse r = new StatsResponse();
        r.setTotalQuota(toBd(dto.getZsxed()));
        r.setUsedQuota(toBd(dto.getYsysxed()));
        r.setRemainingQuota(toBd(dto.getSysxed()));
        r.setInvoiceCount(Integer.parseInt(oz(dto.getZfpzs())));
        return r;
    }

    private String oz(String s) { return s == null || s.isEmpty() ? "0" : s; }
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private BigDecimal toBd(String s) {
        if (s == null || s.isEmpty()) return BigDecimal.ZERO;
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private String normalizeDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        String d = date.trim().replace('T', ' ');
        if (d.length() == 19) return d;
        if (d.length() > 19) return d.substring(0, 19);
        if (d.length() == 16) return d + ":00";
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
