package com.flyemu.share.repository.basic;

import com.flyemu.share.entity.basic.Product;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepositoryImplementation<Product, Long> {

    @Modifying
    @Query("UPDATE Product p SET p.enabled = :enabled WHERE p.id = :id")
    int updateEnabledById(@Param("id") Long id, @Param("enabled") Boolean enabled);
}
