package com.asg.finance.repository;

import com.asg.finance.entity.ArCreditNoteDtl;
import com.asg.finance.entity.key.ArCreditNoteDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ArCreditNoteDtlRepository extends JpaRepository<ArCreditNoteDtl, ArCreditNoteDtlKey> {
    
    List<ArCreditNoteDtl> findByTransactionPoidOrderByDetRowId(Long transactionPoid);
    
    void deleteByTransactionPoid(Long transactionPoid);
}