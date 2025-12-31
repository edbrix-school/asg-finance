package com.asg.finance.repository;

import com.asg.finance.entity.ArCreditNoteChargeDtl;
import com.asg.finance.entity.key.ArCreditNoteChargeDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ArCreditNoteChargeDtlRepository extends JpaRepository<ArCreditNoteChargeDtl, ArCreditNoteChargeDtlKey> {
    
    List<ArCreditNoteChargeDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);
    
    void deleteByTransactionPoid(Long transactionPoid);
}