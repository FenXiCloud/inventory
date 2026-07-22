package com.flyemu.share.repository.basic;

import com.flyemu.share.entity.basic.Customer;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

public interface CustomerRepository extends JpaRepositoryImplementation<Customer,Long> {

}
