package com.flyemu.share.repository;
import java.lang.Integer;
import com.flyemu.share.entity.fund.OrderStaff;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;


/**
 * 持久层
 *
 * @author shuaiqi
 * @since 2025-05-20 11:44:12
 */
public interface OrderStaffRepository extends JpaRepositoryImplementation<OrderStaff, Integer> {

}

