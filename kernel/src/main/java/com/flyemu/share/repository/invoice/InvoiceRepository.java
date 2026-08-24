package com.flyemu.share.repository.invoice;

import com.flyemu.share.entity.invoice.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByMerchantIdOrderByIssueDateDesc(Long merchantId);

    Invoice findByMerchantIdAndThirdPartyNumber(Long merchantId, String thirdPartyNumber);

    List<Invoice> findByMerchantIdAndInvoiceTypeOrderByIssueDateDesc(Long merchantId, String invoiceType);

    Page<Invoice> findAllByMerchantIdOrderByIssueDateDesc(Long merchantId, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.merchantId = :merchantId AND i.invoiceType = :type ORDER BY i.issueDate DESC")
    Page<Invoice> findByMerchantIdAndInvoiceTypeOrderByIssueDateDesc(@Param("merchantId") Long merchantId, @Param("type") String invoiceType, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.merchantId = :merchantId AND " +
           "(:buyerName IS NULL OR i.buyerName LIKE CONCAT('%', :buyerName, '%')) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:invoiceType IS NULL OR i.invoiceType = :invoiceType) " +
           "ORDER BY i.issueDate DESC")
    Page<Invoice> searchInvoices(
            @Param("merchantId") Long merchantId,
            @Param("buyerName") String buyerName,
            @Param("status") String status,
            @Param("invoiceType") String invoiceType,
            Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.merchantId = :merchantId AND (i.invoiceType IS NULL OR i.invoiceType <> 'RED') ORDER BY i.issueDate DESC")
    Page<Invoice> findBlueInvoices(@Param("merchantId") Long merchantId, Pageable pageable);
}
