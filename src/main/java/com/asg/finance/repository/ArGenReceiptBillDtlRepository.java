package com.asg.finance.repository;

import com.asg.finance.entity.ArGenReceiptBillDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArGenReceiptBillDtlRepository extends JpaRepository<ArGenReceiptBillDtl, Long> {

    /**
     * Find all bill details for a given receipt transaction
     */
    List<ArGenReceiptBillDtl> findByReceiptHdr_TransactionPoid(Long transactionPoid);

    /**
     * Find bill detail by transaction POID and detail row ID
     */
    Optional<ArGenReceiptBillDtl> findByTransactionPoidAndDetRowId(Long transactionPoid, Long detRowId);

    /**
     * Delete all bill details for a given receipt transaction
     */
    @Modifying
    @Query("DELETE FROM ArGenReceiptBillDtl d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(@Param("transactionPoid") Long transactionPoid);

    /**
     * Delete bill details by transaction POID and detail row IDs
     */
    @Modifying
    @Query("DELETE FROM ArGenReceiptBillDtl d WHERE d.transactionPoid = :transactionPoid AND d.detRowId IN :detRowIds")
    void deleteByTransactionPoidAndDetRowIdIn(@Param("transactionPoid") Long transactionPoid, @Param("detRowIds") List<Long> detRowIds);

    long countByTransactionPoid(Long transactionPoid);
}

