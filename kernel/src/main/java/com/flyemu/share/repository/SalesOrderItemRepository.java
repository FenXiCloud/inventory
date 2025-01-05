package com.flyemu.share.repository;

import com.flyemu.share.entity.sales.SalesOrderItem;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;


public interface SalesOrderItemRepository extends JpaRepositoryImplementation<SalesOrderItem, Long> {

}
