package com.asg.finance.repository;

import com.asg.finance.entity.ApPurchaseInvoiceHdrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApPurchaseInvoiceHdrRepository extends JpaRepository<ApPurchaseInvoiceHdrEntity, Long> {


    ApPurchaseInvoiceHdrEntity findByTransactionPoid(Long transactionPoid);

}
