package com.flyemu.share.repository;

import com.flyemu.share.dto.OtherFundDetailsVO;
import com.flyemu.share.entity.fund.AccountFlow;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.time.LocalDate;
import java.util.List;


public interface AccountFlowRepository extends JpaRepositoryImplementation<AccountFlow,Long> {


        @Query(value = """
        (
            SELECT '收入' AS fundType,
                oi.order_date AS orderDate,
                oi.order_no AS orderNo,
                oi.order_staff_name AS orderStaffName,
                oii.account_type_name AS accountTypeName,
                oii.amount AS income,
                null AS expense,
                oii.remarks AS remarks,
                oii.source_doc_no AS sourceDocNo,
                oii.source_business_name AS sourceBusinessName,
                oii.source_date AS sourceDate
            FROM jxc_other_income oi
            JOIN jxc_other_income_item oii ON oi.id = oii.other_income_id
            WHERE oi.order_status = '已审核'
        )
        UNION ALL
        (
            SELECT '支出' AS fundType,
                oe.order_date AS orderDate,
                oe.order_no AS orderNo,
                oe.order_staff_name AS orderStaffName,
                oei.account_type_name AS accountTypeName,
                null AS income,
                oei.amount AS expense,
                oei.remarks AS remarks,
                oei.source_doc_no AS sourceDocNo,
                oei.source_business_name AS sourceBusinessName,
                oei.source_date AS sourceDate
            FROM jxc_other_expense oe
            JOIN jxc_other_expense_item oei ON oe.id = oei.other_expense_id
            WHERE oe.order_status = '已审核'
        )
        ORDER BY orderDate DESC
    """, countQuery = "SELECT 1", nativeQuery = true)
        Page<OtherFundDetailsVO> queryOtherFundDetails(
                @Param("merchantId") Long merchantId,
                @Param("accountBookId") Long accountBookId,
                @Param("orderStaffName") String orderStaffName,
                @Param("startTime") LocalDate startTime,
                @Param("endTime") LocalDate endTime,
                Pageable pageable);








//
//
//    @Query(value = """
//    SELECT COUNT(*) FROM (
//        (
//            SELECT 1
//            FROM jxc_other_income oi
//            JOIN jxc_other_income_item oii ON oi.id = oii.other_income_id
//            WHERE
//                oi.order_status='已审核'
//                and
//                (:merchantId IS NULL OR oi.merchant_id = :merchantId)
//              AND (:accountBookId IS NULL OR oi.account_book_id = :accountBookId)
//              AND (:orderStaffName IS NULL OR oi.order_staff_name LIKE CONCAT('%', :orderStaffName, '%'))
//              AND (:startTime IS NULL OR oi.order_date >= :startTime)
//              AND (:endTime IS NULL OR oi.order_date <= :endTime)
//        )
//        UNION ALL
//        (
//            SELECT 1
//            FROM jxc_other_expense oe
//            JOIN jxc_other_expense_item oei ON oe.id = oei.other_expense_id
//            WHERE  oe.order_status='已审核'
//                and (:merchantId IS NULL OR oe.merchant_id = :merchantId)
//              AND (:accountBookId IS NULL OR oe.account_book_id = :accountBookId)
//              AND (:orderStaffName IS NULL OR oe.order_staff_name LIKE CONCAT('%', :orderStaffName, '%'))
//              AND (:startTime IS NULL OR oe.order_date >= :startTime)
//              AND (:endTime IS NULL OR oe.order_date <= :endTime)
//        )
//    ) t
//""", nativeQuery = true)
//    Long countOtherFundDetails(
//            @Param("merchantId") Long merchantId,
//            @Param("accountBookId") Long accountBookId,
//            @Param("orderStaffName") String orderStaffName,
//            @Param("startTime") LocalDate startTime,
//            @Param("endTime") LocalDate endTime);



}



