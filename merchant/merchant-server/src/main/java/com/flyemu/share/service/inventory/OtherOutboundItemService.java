package com.flyemu.share.service.inventory;

import com.flyemu.share.entity.basic.Customer;
import com.flyemu.share.entity.basic.CustomerLevelPrice;
import com.flyemu.share.entity.basic.Product;
import com.flyemu.share.entity.inventory.OtherOutbound;
import com.flyemu.share.entity.inventory.OtherOutboundItem;
import com.flyemu.share.entity.inventory.QOtherOutboundItem;
import com.flyemu.share.repository.CustomerLevelPriceRepository;
import com.flyemu.share.repository.OtherOutboundItemRepository;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.basic.CustomerLevelPriceService;
import com.flyemu.share.service.basic.CustomerLevelService;
import com.flyemu.share.service.basic.CustomerService;
import com.flyemu.share.service.basic.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * @功能描述: 其他出库单明细
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OtherOutboundItemService extends AbsService {

    private final static QOtherOutboundItem qOtherOutboundItem = QOtherOutboundItem.otherOutboundItem;

    private final CustomerService customerService;

    private final CustomerLevelPriceService customerLevelPriceService;

    private final ProductService productService;

    private final OtherOutboundItemRepository otherOutboundItemRepository;

    /**
     * 其他出库单明细处理
     *
     * @param otherOutbound      其他出库信息
     * @param otherOutboundItems 其他出库单列表
     */
    public BigDecimal generateOutboundDetails(OtherOutbound otherOutbound, List<OtherOutboundItem> otherOutboundItems) {
        Long otherOutboundId = otherOutbound.getId();
        Long merchantId = otherOutbound.getMerchantId();
        Long accountBookId = otherOutbound.getAccountBookId();
        String orderNo = otherOutbound.getOrderNo();
        // 清除历史关联
        this.deleteByOtherOutboundId(otherOutboundId);
        Long customerId = otherOutbound.getCustomerId();
        final BigDecimal[] totalAmount = {BigDecimal.ZERO};
        otherOutboundItems.forEach(otherOutboundItem -> {
            Long productId = otherOutboundItem.getProductId();
            Double quantity = otherOutboundItem.getQuantity();
            otherOutboundItem.setOtherOutboundId(otherOutboundId);
            otherOutboundItem.setMerchantId(merchantId);
            otherOutboundItem.setAccountBookId(accountBookId);
            otherOutboundItem.setCreatedAt(otherOutbound.getCreatedAt());
            otherOutboundItem.setCreatedBy(otherOutbound.getCreatedBy());
            // 获取商品出库单位成本
            BigDecimal otherOutboundUnitPrice = this.findOtherOutboundUnitPrice(productId, customerId, merchantId);
            otherOutboundItem.setUnitPrice(otherOutboundUnitPrice);
            otherOutboundItem.setSubtotal(otherOutboundUnitPrice.multiply(new BigDecimal(quantity)));
            totalAmount[0] = totalAmount[0].add(otherOutboundItem.getSubtotal());
            otherOutboundItem.setProductId(productId);
            otherOutboundItem.setBatchNumber(orderNo);
        });
        if (!otherOutboundItems.isEmpty()) {
            otherOutboundItemRepository.saveAll(otherOutboundItems);
        }
        return totalAmount[0].setScale(2, RoundingMode.DOWN);
    }

    /**
     * 获取商品出库单位成本
     *
     * @param productId  商品id
     * @param customerId 客户id
     * @param merchantId 商户id
     * @return unitPrice
     */
    private BigDecimal findOtherOutboundUnitPrice(Long productId, Long customerId, Long merchantId) {
        //todo 需调整获取商品出库单位成本
        BigDecimal otherOutboundUnitPrice;
        if (customerId != null) {
            Customer customer = customerService.selectByPrimaryKey(customerId);
            if (customer != null) {
                CustomerLevelPrice customerLevelPrice = customerLevelPriceService.findByProductIdAndCustomerLevelId(productId, customer.getCustomerLevelId());
                if (customerLevelPrice != null) {
                    otherOutboundUnitPrice = customerLevelPrice.getPrice();
                    return otherOutboundUnitPrice;
                }
            }
        }
        Product product = productService.loadById(productId, merchantId);
        otherOutboundUnitPrice = product.getPurchasePrice();
        return otherOutboundUnitPrice;
    }

    /**
     * 根据其他出库单id清除明细信息
     *
     * @param otherOutboundId 其他出库单id
     */
    public void deleteByOtherOutboundId(Long otherOutboundId) {
        jqf.delete(qOtherOutboundItem).where(qOtherOutboundItem.otherOutboundId.eq(otherOutboundId)).execute();
    }

    /**
     * 根据出库单id获取出库单明细
     *
     * @param otherOutboundId 出库单id
     * @return otherOutboundItem list
     */
    public List<OtherOutboundItem> findByOtherOutboundId(Long otherOutboundId) {
        return jqf.selectFrom(qOtherOutboundItem).where(qOtherOutboundItem.otherOutboundId.eq(otherOutboundId)).fetch();
    }
}
