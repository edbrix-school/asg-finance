package com.asg.finance.repository;

import com.asg.finance.entity.ChequeReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChequeReturnRepository extends JpaRepository<ChequeReturn, Long> {
}
