package com.flyemu.share.service.basic;

import cn.hutool.core.collection.CollUtil;
import com.flyemu.share.dto.AuxiliaryUnitPrice;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.Tuple;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品辅助单位（多单位）批量读取，供采购订单/采购入库等单据加载明细时回填单位下拉选项。
 * 仅当商品启用辅助单位且列表非空时返回，保证前端行内"采购单位"下拉可见、可切换。
 */
@Service
public class ProductAuxiliaryUnitService extends BaseService {

    private static final QProduct qProduct = QProduct.product;

    /**
     * 批量读取商品辅助单位（仅 enableMultiUnit=true 且列表非空的商品返回，做防御性拷贝，不改写持久化实体 JSON）。
     */
    public Map<Long, List<AuxiliaryUnitPrice>> loadAuxUnits(Collection<Long> productIds, Long merchantId) {
        Map<Long, List<AuxiliaryUnitPrice>> result = new HashMap<>();
        if (CollUtil.isEmpty(productIds)) {
            return result;
        }
        for (Tuple t : bqf.selectFrom(qProduct)
                .select(qProduct.id, qProduct.enableMultiUnit, qProduct.auxiliaryUnitPrices)
                .where(qProduct.id.in(productIds).and(qProduct.merchantId.eq(merchantId)))
                .fetch()) {
            Long pid = t.get(qProduct.id);
            if (Boolean.TRUE.equals(t.get(qProduct.enableMultiUnit))) {
                List<AuxiliaryUnitPrice> aux = t.get(qProduct.auxiliaryUnitPrices);
                if (CollUtil.isNotEmpty(aux)) {
                    result.put(pid, new ArrayList<>(aux));
                }
            }
        }
        return result;
    }

    /**
     * 在辅助单位列表最前面插入基本单位（rate=1，unitPrice=该单据行的基本单价，保持与源单/基本单位口径一致）。
     * aux 为空时返回 null，前端该行单位仅以文本展示（无多单位商品保持不变）。
     */
    public static List<AuxiliaryUnitPrice> withBase(Long baseUnitId, String baseUnitName,
                                                    BigDecimal baseUnitPrice, List<AuxiliaryUnitPrice> aux) {
        if (CollUtil.isEmpty(aux)) {
            return null;
        }
        List<AuxiliaryUnitPrice> out = new ArrayList<>(aux.size() + 1);
        out.add(new AuxiliaryUnitPrice(baseUnitId, baseUnitName, 1d,
                baseUnitPrice == null ? BigDecimal.ZERO : baseUnitPrice));
        out.addAll(aux);
        return out;
    }
}
