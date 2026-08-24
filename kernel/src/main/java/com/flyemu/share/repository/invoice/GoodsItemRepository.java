package com.flyemu.share.repository.invoice;

import com.flyemu.share.entity.invoice.GoodsItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GoodsItemRepository extends JpaRepository<GoodsItem, Long> {

    Optional<GoodsItem> findByMerchantIdAndGoodsCode(Long merchantId, String goodsCode);

    @Query("SELECT g FROM GoodsItem g WHERE g.merchantId = :merchantId AND " +
           "(:keyword IS NULL OR g.goodsName LIKE %:keyword% OR g.goodsCode LIKE %:keyword%) " +
           "ORDER BY g.isDefault DESC, g.updatedAt DESC")
    Page<GoodsItem> search(@Param("merchantId") Long merchantId, @Param("keyword") String keyword, Pageable pageable);
}
