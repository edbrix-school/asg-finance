package com.asg.finance.repository;

import com.asg.finance.entity.ArGenReceiptAdvanceDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArGenReceiptAdvanceDtlRepository extends JpaRepository<ArGenReceiptAdvanceDtl, Long> {

    /**
     * Find all advance details for a given receipt transaction
     */
    List<ArGenReceiptAdvanceDtl> findByReceiptHdr_TransactionPoid(Long transactionPoid);

    /**
     * Delete all advance details for a given receipt transaction
     */
    void deleteByReceiptHdr_TransactionPoid(Long transactionPoid);

    long countByTransactionPoid(Long transactionPoid);
}

