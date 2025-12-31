package com.asg.finance.repository;

import com.asg.finance.entity.GLPaymentVoucherHDREntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankPaymentVoucherRepository extends JpaRepository<GLPaymentVoucherHDREntity, Long> {

}
