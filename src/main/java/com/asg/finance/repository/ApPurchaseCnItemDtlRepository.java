package com.asg.finance.repository;

import com.asg.finance.entity.ApPurchaseCnItemDtl;
import com.asg.finance.entity.key.ApPurchaseCnItemDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApPurchaseCnItemDtlRepository extends JpaRepository<ApPurchaseCnItemDtl, ApPurchaseCnItemDtlKey> {
    List<ApPurchaseCnItemDtl> findByTransactionPoid(Long transactionPoid);
    void deleteByTransactionPoid(Long transactionPoid);
}
