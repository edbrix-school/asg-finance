package com.asg.finance.repository;

import com.asg.finance.entity.GlBankFileDtl;
import com.asg.finance.entity.key.GlBankFileDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlBankFileDtlRepository extends JpaRepository<GlBankFileDtl, GlBankFileDtlKey> {
    List<GlBankFileDtl> findByTransactionPoid(Long transactionPoid);

    @Modifying
    @Query("DELETE FROM GlBankFileDtl d WHERE d.transactionPoid = :transactionPoid")
    void deleteByTransactionPoid(Long transactionPoid);
    
    @Query("SELECT d FROM GlBankFileDtl d WHERE d.transactionPoid = :transactionPoid AND d.deleted = :deleted")
    List<GlBankFileDtl> findByTransactionPoidAndDeleted(Long transactionPoid, String deleted);
    
    @Query("SELECT DISTINCT d.debitCompanyPoid FROM GlBankFileDtl d WHERE d.transactionPoid = :transactionPoid AND d.debitTransactionPoid IS NOT NULL")
    List<Long> findDistinctDebitCompanyPoidsByTransactionPoid(Long transactionPoid);
    
    @Query("SELECT DISTINCT d.debitCompanyPoid FROM GlBankFileDtl d WHERE d.transactionPoid = :transactionPoid AND d.debitTransactionPoid IS NOT NULL AND d.debitTransactionPoid <> 0")
    List<Long> findDistinctDebitCompanyPoidByTransactionPoid(Long transactionPoid);
    
    @Modifying
    @Query("UPDATE GlBankFileDtl d SET d.deleted = :deleted WHERE d.transactionPoid = :transactionPoid AND d.debitDocRef = :debitDocRef")
    void updateDeletedStatus(Long transactionPoid, String debitDocRef, String deleted);
    
    @Modifying
    @Query("UPDATE GlBankFileDtl d SET d.deleted = :deleted WHERE d.transactionPoid = :transactionPoid AND d.debitDocRef = :docRef")
    void updateDeleted(Long transactionPoid, String docRef, String deleted);
    
    @Modifying
    @Query("DELETE FROM GlBankFileDtl d WHERE d.transactionPoid = :transactionPoid AND d.deleted = :deleted")
    void deleteByTransactionPoidAndDeleted(Long transactionPoid, String deleted);
    
    @Query("SELECT d FROM GlBankFileDtl d WHERE d.transactionPoid = :transactionPoid AND d.debitCompanyPoid = :companyPoid AND d.deleted = :deleted")
    List<GlBankFileDtl> findByTransactionPoidCompanyAndDeleted(Long transactionPoid, Long companyPoid, String deleted);
}