package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.ProductCombo;
import com.flyemu.share.entity.basic.ProductComboItem;
import com.flyemu.share.entity.basic.QProductCombo;
import com.flyemu.share.entity.basic.QProductComboItem;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.ProductComboForm;
import com.flyemu.share.repository.basic.ProductComboItemRepository;
import com.flyemu.share.repository.basic.ProductComboRepository;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.way.CodeGenerator;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductComboService extends BaseService {

    private final static QProductCombo qCombo = QProductCombo.productCombo;
    private final static QProductComboItem qItem = QProductComboItem.productComboItem;

    private final ProductComboRepository productComboRepository;
    private final ProductComboItemRepository productComboItemRepository;

    public PageResults<ProductCombo> list(Page page, String filter, Long merchantId, Long accountBookId) {
        BooleanBuilder b = new BooleanBuilder();
        b.and(qCombo.merchantId.eq(merchantId));
        b.and(qCombo.accountBookId.eq(accountBookId));
        if (StrUtil.isNotBlank(filter)) {
            b.and(qCombo.name.contains(filter.trim()).or(qCombo.code.contains(filter.trim())));
        }
        JPAQuery<ProductCombo> q = jqf.selectFrom(qCombo).where(b).orderBy(qCombo.id.desc());
        long total = q.fetchCount();
        List<ProductCombo> list = q.offset(page.getOffset()).limit(page.getPageSize()).fetch();
        return new PageResults<>(list, page, total);
    }

    public ProductComboForm load(Long id, Long merchantId, Long accountBookId) {
        ProductCombo combo = jqf.selectFrom(qCombo)
                .where(qCombo.id.eq(id).and(qCombo.merchantId.eq(merchantId)).and(qCombo.accountBookId.eq(accountBookId)))
                .fetchFirst();
        if (combo == null) {
            throw new ServiceException("套餐不存在");
        }
        List<ProductComboItem> items = jqf.selectFrom(qItem)
                .where(qItem.comboId.eq(id).and(qItem.merchantId.eq(merchantId)).and(qItem.accountBookId.eq(accountBookId)))
                .orderBy(qItem.id.asc())
                .fetch();
        ProductComboForm form = new ProductComboForm();
        form.setCombo(combo);
        form.setComboItemList(items);
        return form;
    }

    @Transactional
    public void save(ProductComboForm form, Long merchantId, Long accountBookId) {
        ProductCombo combo = form.getCombo();
        if (combo == null) {
            throw new ServiceException("套餐参数错误");
        }
        if (StrUtil.isBlank(combo.getName())) {
            throw new ServiceException("套餐名称不能为空");
        }
        if (combo.getId() != null) {
            ProductCombo original = productComboRepository.findById(combo.getId())
                    .orElseThrow(() -> new ServiceException("套餐不存在"));
            BeanUtil.copyProperties(combo, original, CopyOptions.create().ignoreNullValue());
            original.setUpdatedAt(LocalDateTime.now());
            combo = productComboRepository.save(original);
        } else {
            if (StrUtil.isBlank(combo.getCode())) {
                combo.setCode(CodeGenerator.generateCode());
            }
            combo.setAccountBookId(accountBookId);
            combo.setMerchantId(merchantId);
            if (combo.getEnabled() == null) {
                combo.setEnabled(true);
            }
            combo.setCreatedAt(LocalDateTime.now());
            combo.setUpdatedAt(LocalDateTime.now());
            combo = productComboRepository.save(combo);
        }

        // 先删旧明细再插入
        Long comboId = combo.getId();
        jqf.delete(qItem)
                .where(qItem.comboId.eq(comboId)
                        .and(qItem.merchantId.eq(merchantId))
                        .and(qItem.accountBookId.eq(accountBookId)))
                .execute();

        List<ProductComboItem> items = form.getComboItemList();
        if (items != null && !items.isEmpty()) {
            for (ProductComboItem item : items) {
                item.setId(null);
                item.setComboId(comboId);
                item.setMerchantId(merchantId);
                item.setAccountBookId(accountBookId);
                productComboItemRepository.save(item);
            }
        }
    }

    @Transactional
    public void remove(Long id, Long merchantId, Long accountBookId) {
        jqf.delete(qItem)
                .where(qItem.comboId.eq(id)
                        .and(qItem.merchantId.eq(merchantId))
                        .and(qItem.accountBookId.eq(accountBookId)))
                .execute();
        jqf.delete(qCombo)
                .where(qCombo.id.eq(id)
                        .and(qCombo.merchantId.eq(merchantId))
                        .and(qCombo.accountBookId.eq(accountBookId)))
                .execute();
    }
}
