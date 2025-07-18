package com.flyemu.share.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 *@author shuaiqi
 */

@Repository
public class CategoryTreeRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public List<Long> getAllSubCategoryIds(Long categoryId) {
        String sql = "WITH RECURSIVE cte AS ( " +
                "   SELECT id FROM jxc_product_category WHERE id = :categoryId " +
                "   UNION ALL " +
                "   SELECT pc.id FROM jxc_product_category pc " +
                "   INNER JOIN cte ON pc.pid = cte.id " +
                ") SELECT id FROM cte;";

        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("categoryId", categoryId);

        return nativeQuery.getResultList();
    }
}
