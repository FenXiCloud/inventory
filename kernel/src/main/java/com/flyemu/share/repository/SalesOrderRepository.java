package com.flyemu.share.repository;

import com.flyemu.share.entity.sales.SalesOrder;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;


public interface SalesOrderRepository extends JpaRepositoryImplementation<SalesOrder, Long> {

}
