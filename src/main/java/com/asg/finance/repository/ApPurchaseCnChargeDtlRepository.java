package com.asg.finance.repository;

import com.asg.finance.entity.ApPurchaseCnChargeDtl;
import com.asg.finance.entity.key.ApPurchaseCnChargeDtlKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApPurchaseCnChargeDtlRepository extends JpaRepository<ApPurchaseCnChargeDtl, ApPurchaseCnChargeDtlKey> {
    List<ApPurchaseCnChargeDtl> findByTransactionPoid(Long transactionPoid);
    void deleteByTransactionPoid(Long transactionPoid);
}
