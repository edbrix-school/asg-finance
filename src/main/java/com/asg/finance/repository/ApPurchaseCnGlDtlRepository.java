package com.asg.finance.repository;

import com.asg.finance.entity.ApPurchaseCnGlDtl;
import com.asg.finance.entity.key.ApPurchaseCnGlDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApPurchaseCnGlDtlRepository extends JpaRepository<ApPurchaseCnGlDtl, ApPurchaseCnGlDtlKey> {
    List<ApPurchaseCnGlDtl> findByTransactionPoid(Long transactionPoid);
    void deleteByTransactionPoid(Long transactionPoid);
}
