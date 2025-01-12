package com.flyemu.share.repository;

import com.flyemu.share.entity.fund.Account;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;


public interface AccountRepository extends JpaRepositoryImplementation<Account,Long> {

}
