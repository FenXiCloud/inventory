package com.flyemu.share.service.inventory;

import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.inventory.ProductSerial;
import com.flyemu.share.entity.inventory.QProductSerial;
import com.flyemu.share.repository.inventory.ProductSerialRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品序列号管理：登记、追溯查询、出库核销、报废。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductSerialService extends BaseService {

    private final static QProductSerial qProductSerial = QProductSerial.productSerial;
    private final static QProduct qProduct = QProduct.product;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;

    private final ProductSerialRepository productSerialRepository;

    /**
     * 批量登记序列号（按逗号/换行/分号分隔），已存在的序列号自动跳过。
     */
    @Transactional
    public int register(Long merchantId, Long accountBookId, Long productId, Long warehouseId,
                        String batchNumber, String serialNumbers, LocalDate inboundDate, String remark) {
        String[] nums = (serialNumbers == null ? "" : serialNumbers).split("[,\\n;，；]+");
        int count = 0;
        for (String raw : nums) {
            String sn = raw == null ? "" : raw.trim();
            if (sn.isEmpty()) continue;
            if (productSerialRepository.findByMerchantIdAndSerialNumber(merchantId, sn).isPresent()) {
                continue;
            }
            ProductSerial s = new ProductSerial();
            s.setSerialNumber(sn);
            s.setProductId(productId);
            s.setWarehouseId(warehouseId);
            s.setBatchNumber(batchNumber);
            s.setStatus("在库");
            s.setInboundDate(inboundDate != null ? inboundDate : LocalDate.now());
            s.setRemark(remark);
            s.setAccountBookId(accountBookId);
            s.setMerchantId(merchantId);
            s.setCreatedAt(LocalDateTime.now());
            productSerialRepository.save(s);
            count++;
        }
        return count;
    }

    /**
     * 序列号追溯查询，补齐商品/仓库名称。
     */
    public List<Map<String, Object>> list(Long merchantId, Long accountBookId, String status, Long productId, String keyword) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qProductSerial.merchantId.eq(merchantId));
        builder.and(qProductSerial.accountBookId.eq(accountBookId));
        if (StringUtils.hasText(status)) builder.and(qProductSerial.status.eq(status));
        if (productId != null) builder.and(qProductSerial.productId.eq(productId));
        if (StringUtils.hasText(keyword)) builder.and(qProductSerial.serialNumber.like("%" + keyword + "%"));

        List<Tuple> tuples = bqf.selectFrom(qProductSerial)
                .select(qProductSerial, qProduct.code, qProduct.name, qProduct.specification, qWarehouse.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qProductSerial.productId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qProductSerial.warehouseId))
                .where(builder)
                .orderBy(qProductSerial.id.desc())
                .fetch();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : tuples) {
            ProductSerial s = t.get(qProductSerial);
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("serialNumber", s.getSerialNumber());
            m.put("productId", s.getProductId());
            m.put("productCode", t.get(qProduct.code));
            m.put("productName", t.get(qProduct.name));
            m.put("productSpecification", t.get(qProduct.specification));
            m.put("warehouseId", s.getWarehouseId());
            m.put("warehouseName", t.get(qWarehouse.name));
            m.put("batchNumber", s.getBatchNumber());
            m.put("status", s.getStatus());
            m.put("inboundDate", s.getInboundDate());
            m.put("outboundDate", s.getOutboundDate());
            m.put("inboundOrderId", s.getInboundOrderId());
            m.put("outboundOrderId", s.getOutboundOrderId());
            m.put("remark", s.getRemark());
            result.add(m);
        }
        return result;
    }

    /**
     * 出库核销：把「在库」序列号置为「已出库」并记录出库单与日期。
     */
    @Transactional
    public int outbound(Long merchantId, List<Long> ids, Long outboundOrderId, LocalDate outboundDate) {
        int count = 0;
        for (Long id : ids) {
            ProductSerial s = productSerialRepository.findById(id)
                    .filter(x -> merchantId.equals(x.getMerchantId()))
                    .orElse(null);
            if (s == null || !"在库".equals(s.getStatus())) continue;
            s.setStatus("已出库");
            s.setOutboundOrderId(outboundOrderId);
            s.setOutboundDate(outboundDate != null ? outboundDate : LocalDate.now());
            productSerialRepository.save(s);
            count++;
        }
        return count;
    }

    /**
     * 报废：把「在库」序列号置为「报废」。
     */
    @Transactional
    public int scrap(Long merchantId, List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            ProductSerial s = productSerialRepository.findById(id)
                    .filter(x -> merchantId.equals(x.getMerchantId()))
                    .orElse(null);
            if (s == null || !"在库".equals(s.getStatus())) continue;
            s.setStatus("报废");
            productSerialRepository.save(s);
            count++;
        }
        return count;
    }

    @Transactional
    public void delete(Long merchantId, Long id) {
        productSerialRepository.findById(id)
                .filter(x -> merchantId.equals(x.getMerchantId()))
                .ifPresent(productSerialRepository::delete);
    }
}
