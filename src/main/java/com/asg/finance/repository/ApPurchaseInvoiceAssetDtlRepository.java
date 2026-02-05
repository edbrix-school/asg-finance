package com.asg.finance.repository;

import com.asg.finance.entity.ApPurchaseInvoiceAssetDtlEntity;
import com.asg.finance.entity.key.ApPurchaseInvoiceAssetDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApPurchaseInvoiceAssetDtlRepository extends JpaRepository<ApPurchaseInvoiceAssetDtlEntity, ApPurchaseInvoiceAssetDtlKey> {

    List<ApPurchaseInvoiceAssetDtlEntity> findByIdTransactionPoid(Long transactionPoid);

    @Query("SELECT COALESCE(MAX(e.id.detRowId) + 1, 1) FROM ApPurchaseInvoiceAssetDtlEntity e WHERE e.id.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    void deleteByIdTransactionPoid(Long transactionPoid);

    @Query("""
    select a.id.detRowId
    from ApPurchaseInvoiceAssetDtlEntity a
    where a.id.transactionPoid = :transactionPoid
""")
    List<Long> findDetRowIdsByTransactionPoid(@Param("transactionPoid") Long transactionPoid);
}
