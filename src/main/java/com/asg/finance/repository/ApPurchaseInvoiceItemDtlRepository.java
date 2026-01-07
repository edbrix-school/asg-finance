package com.asg.finance.repository;

import com.asg.finance.entity.ApPurchaseInvoiceItemDtlEntity;
import com.asg.finance.entity.key.ApPurchaseInvoiceItemDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApPurchaseInvoiceItemDtlRepository extends JpaRepository<ApPurchaseInvoiceItemDtlEntity, ApPurchaseInvoiceItemDtlKey> {

    List<ApPurchaseInvoiceItemDtlEntity> findByIdTransactionPoid(Long transactionPoid);

    @Query("SELECT MAX(e.id.detRowId) FROM ApPurchaseInvoiceItemDtlEntity e WHERE e.id.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    void deleteByIdTransactionPoid(Long transactionPoid);
}
