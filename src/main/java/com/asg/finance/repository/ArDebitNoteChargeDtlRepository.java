package com.asg.finance.repository;

import com.asg.finance.entity.ArDebitNoteChargeDtl;
import com.asg.finance.entity.ArDebitNoteChargeDtlId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArDebitNoteChargeDtlRepository extends JpaRepository<ArDebitNoteChargeDtl, ArDebitNoteChargeDtlId> {

    List<ArDebitNoteChargeDtl> findByTransactionPoid(Long transactionPoid);

    void deleteByTransactionPoid(Long transactionPoid);

    @Query("SELECT MAX(d.detRowId) FROM ArDebitNoteChargeDtl d WHERE d.transactionPoid = :transactionPoid")
    Long findMaxDetRowIdByTransactionPoid(Long transactionPoid);
}