package com.asg.finance.repository;

import com.asg.finance.entity.ApPurchaseCnHdr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApPurchaseCnHdrRepository extends JpaRepository<ApPurchaseCnHdr, Long> {
}
