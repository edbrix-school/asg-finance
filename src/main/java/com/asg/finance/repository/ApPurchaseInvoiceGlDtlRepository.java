package com.asg.finance.repository;

import com.asg.finance.entity.ApPurchaseInvoiceGlDtlEntity;
import com.asg.finance.entity.key.ApPurchaseInvoiceGlDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApPurchaseInvoiceGlDtlRepository extends JpaRepository<ApPurchaseInvoiceGlDtlEntity, ApPurchaseInvoiceGlDtlKey> {

    List<ApPurchaseInvoiceGlDtlEntity> findByIdTransactionPoid(Long transactionPoid);

     @Query("SELECT COALESCE(MAX(e.id.detRowId) + 1, 1) FROM ApPurchaseInvoiceGlDtlEntity e WHERE e.id.transactionPoid = :transactionPoid")
     Long findMaxDetRowIdByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

     void deleteByIdTransactionPoid(Long transactionPoid);

     void deleteByIdTransactionPoidAndIdDetRowId(Long transactionPoid, Long detRowId);
}
