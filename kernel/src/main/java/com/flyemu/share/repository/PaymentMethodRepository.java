package com.flyemu.share.repository;

import com.flyemu.share.entity.basic.PaymentMethod;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;


public interface PaymentMethodRepository extends JpaRepositoryImplementation<PaymentMethod,Long> {

}
