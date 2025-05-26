package com.flyemu.share.repository;

import com.flyemu.share.entity.setting.FinanceItemMap;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;


public interface FinanceItemMapRepository extends JpaRepositoryImplementation<FinanceItemMap, Long> {

    List<FinanceItemMap> findByInventoryId(Long inventoryId);

    List<FinanceItemMap> findByInventoryIdAndCategoryId(Long inventoryId, String categoryId);

    List<FinanceItemMap> findByCategoryType(String categoryType);

    List<FinanceItemMap> findByCategoryId(String categoryId);

    List<FinanceItemMap> findByCategoryIdAndInventoryId(String categoryId, Long inventoryId);
}
