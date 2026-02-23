package com.asg.finance.repository;

import com.asg.finance.entity.ArGenReceiptHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArGenReceiptHdrRepository extends JpaRepository<ArGenReceiptHdr, Long>, JpaSpecificationExecutor<ArGenReceiptHdr> {

    /**
     * Find receipt by document reference (with duplicate protection)
     */
    @Query(value = "SELECT * FROM AR_GEN_RECEIPT_HDR " +
           "WHERE DOC_REF = :docRef " +
           "AND (DELETED IS NULL OR DELETED = 'N') " +
           "ORDER BY TRANSACTION_POID DESC " +
           "FETCH FIRST 1 ROW ONLY", nativeQuery = true)
    Optional<ArGenReceiptHdr> findByDocRef(@Param("docRef") String docRef);

    /**
     * Find receipt by document reference
     * Note: Child collections are fetched separately to avoid MultipleBagFetchException
     */
    @Query(value = "SELECT * FROM AR_GEN_RECEIPT_HDR " +
           "WHERE DOC_REF = :docRef " +
           "AND (DELETED IS NULL OR DELETED = 'N') " +
           "FETCH FIRST 1 ROW ONLY", nativeQuery = true)
    Optional<ArGenReceiptHdr> findByDocRefWithDetails(@Param("docRef") String docRef);

    /**
     * Check if receipt exists by document reference
     */
    boolean existsByDocRef(String docRef);

    /**
     * Count receipts by document reference (to detect duplicates)
     */
    @Query(value = "SELECT COUNT(*) FROM AR_GEN_RECEIPT_HDR " +
           "WHERE DOC_REF = :docRef " +
           "AND (DELETED IS NULL OR DELETED = 'N')", nativeQuery = true)
    Long countByDocRef(@Param("docRef") String docRef);

    /**
     * Find all receipts by document reference (for duplicate detection)
     */
    @Query(value = "SELECT * FROM AR_GEN_RECEIPT_HDR " +
           "WHERE DOC_REF = :docRef " +
           "AND (DELETED IS NULL OR DELETED = 'N') " +
           "ORDER BY TRANSACTION_POID DESC", nativeQuery = true)
    List<ArGenReceiptHdr> findAllByDocRef(@Param("docRef") String docRef);

    /**
     * Get next sequence value for AR_GEN_RECEIPT_HDR_SEQ
     */
    @Query(value = "SELECT AR_GEN_RECEIPT_HDR_SEQ.NEXTVAL FROM DUAL", nativeQuery = true)
    Long getNextSequenceValue();

    /**
     * Find receipt by transaction POID
     * Note: Child collections are fetched separately to avoid MultipleBagFetchException
     */
    @Query(value = "SELECT * FROM AR_GEN_RECEIPT_HDR " +
           "WHERE TRANSACTION_POID = :transactionPoid " +
           "FETCH FIRST 1 ROW ONLY", nativeQuery = true)
    Optional<ArGenReceiptHdr> findByTransactionPoidWithDetails(@Param("transactionPoid") Long transactionPoid);
}

