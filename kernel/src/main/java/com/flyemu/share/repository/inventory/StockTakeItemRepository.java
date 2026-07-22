package com.flyemu.share.repository.inventory;

import com.flyemu.share.entity.inventory.StockTakeItem;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

public interface StockTakeItemRepository extends JpaRepositoryImplementation<StockTakeItem, Long> {

}
