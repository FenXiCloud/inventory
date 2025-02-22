package com.flyemu.share.service.inventory;

import com.flyemu.share.entity.inventory.OtherInbound;
import com.flyemu.share.entity.inventory.OtherInboundItem;
import com.flyemu.share.entity.inventory.QOtherInboundItem;
import com.flyemu.share.repository.OtherInboundItemRepository;
import com.flyemu.share.service.AbsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * @功能描述: 其他入库单明细
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OtherInboundItemService extends AbsService {

    private final static QOtherInboundItem qOtherInboundItem = QOtherInboundItem.otherInboundItem;

    private final OtherInboundItemRepository otherInboundItemRepository;


    /**
     * 其他入库明细
     *
     * @param otherInbound      其他入库对象
     * @param otherInboundItems 其他入库明细列表
     * @return totalAmount
     */
    public BigDecimal generateInboundDetails(OtherInbound otherInbound, List<OtherInboundItem> otherInboundItems) {
        Long otherInboundId = otherInbound.getId();
        Long merchantId = otherInbound.getMerchantId();
        Long accountBookId = otherInbound.getAccountBookId();
        String orderNo = otherInbound.getOrderNo();
        // 清除历史关联
        this.deleteByOtherInboundId(otherInboundId);
        final BigDecimal[] totalAmount = {BigDecimal.ZERO};
        otherInboundItems.forEach(otherInboundItem -> {
            Long productId = otherInboundItem.getProductId();
            otherInboundItem.setOtherInboundId(otherInboundId);
            otherInboundItem.setMerchantId(merchantId);
            otherInboundItem.setAccountBookId(accountBookId);
            otherInboundItem.setCreatedAt(otherInbound.getCreatedAt());
            otherInboundItem.setCreatedBy(otherInbound.getCreatedBy());
            totalAmount[0] = totalAmount[0].add(otherInboundItem.getSubtotal());
            otherInboundItem.setProductId(productId);
            otherInboundItem.setBatchNumber(orderNo);
        });
        if (!otherInboundItems.isEmpty()) {
            otherInboundItemRepository.saveAll(otherInboundItems);
        }
        return totalAmount[0].setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * 根据其他入库单id清除明细信息
     *
     * @param otherInboundId 其他入库单id
     */
    public void deleteByOtherInboundId(Long otherInboundId) {
        jqf.delete(qOtherInboundItem).where(qOtherInboundItem.otherInboundId.eq(otherInboundId)).execute();
    }

    /**
     * 根据入库单id获取出库单明细
     *
     * @param otherInboundId 入库单id
     * @return otherInboundItem list
     */
    public List<OtherInboundItem> findByOtherInboundId(Long otherInboundId) {
        return jqf.selectFrom(qOtherInboundItem).where(qOtherInboundItem.otherInboundId.eq(otherInboundId)).fetch();
    }
}
