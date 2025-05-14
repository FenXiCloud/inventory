package com.flyemu.share.repository;


import com.flyemu.share.entity.setting.AccountBookParameters;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

/**
 * 账套参数设置持久层
 *
 * @author shuaiqi
 * @since 2025-05-13 15:35:38
 */
public interface AccountBookParametersRepository extends JpaRepositoryImplementation<AccountBookParameters, Integer> {

}

