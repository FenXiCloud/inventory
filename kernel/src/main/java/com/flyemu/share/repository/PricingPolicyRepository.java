package com.flyemu.share.repository;

import com.flyemu.share.entity.setting.AccountBook;
import com.flyemu.share.entity.setting.PricingPolicy;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;


public interface PricingPolicyRepository extends JpaRepositoryImplementation<PricingPolicy,Long> {

}
