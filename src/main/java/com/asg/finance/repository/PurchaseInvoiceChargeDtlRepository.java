package com.asg.finance.repository;

import com.asg.finance.entity.PurchaseInvoiceChargeDtl;
import com.asg.finance.entity.PurchaseInvoiceChargeDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseInvoiceChargeDtlRepository extends JpaRepository<PurchaseInvoiceChargeDtl, PurchaseInvoiceChargeDtlId> {

    @Query("SELECT COALESCE(MAX(c.id.detRowId), 0) " +
            "FROM PurchaseInvoiceChargeDtl c " +
            "WHERE c.id.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    List<PurchaseInvoiceChargeDtl> findByIdTransactionPoid(Long transactionPoid);

    void deleteByIdTransactionPoidAndIdDetRowId(Long transactionPoid, Long detRowId);
}
