package com.flyemu.share.service.basic;

import com.flyemu.share.entity.basic.ProductAttribute;
import com.flyemu.share.entity.basic.QProductAttribute;
import com.flyemu.share.repository.basic.ProductAttributeRepository;
import com.flyemu.share.service.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 辅助属性字典（颜色/尺码等多规格属性）：属性名 + 属性值列表。
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductAttributeService extends BaseService {

    private final static QProductAttribute qProductAttribute = QProductAttribute.productAttribute;

    private final ProductAttributeRepository productAttributeRepository;

    public List<ProductAttribute> list(Long merchantId, Long accountBookId) {
        return jqf.selectFrom(qProductAttribute)
                .where(qProductAttribute.merchantId.eq(merchantId)
                        .and(qProductAttribute.accountBookId.eq(accountBookId)))
                .orderBy(qProductAttribute.id.asc())
                .fetch();
    }

    @Transactional
    public ProductAttribute save(ProductAttribute attribute, Long merchantId, Long accountBookId) {
        if (attribute.getId() != null) {
            ProductAttribute original = productAttributeRepository.findById(attribute.getId())
                    .orElseThrow(() -> new RuntimeException("属性不存在"));
            original.setName(attribute.getName());
            original.setValues(attribute.getValues());
            original.setUpdatedAt(LocalDateTime.now());
            return productAttributeRepository.save(original);
        }
        attribute.setId(null);
        attribute.setMerchantId(merchantId);
        attribute.setAccountBookId(accountBookId);
        attribute.setCreatedAt(LocalDateTime.now());
        attribute.setUpdatedAt(LocalDateTime.now());
        return productAttributeRepository.save(attribute);
    }

    @Transactional
    public void remove(Long id, Long merchantId, Long accountBookId) {
        jqf.delete(qProductAttribute)
                .where(qProductAttribute.id.eq(id)
                        .and(qProductAttribute.merchantId.eq(merchantId))
                        .and(qProductAttribute.accountBookId.eq(accountBookId)))
                .execute();
    }
}
