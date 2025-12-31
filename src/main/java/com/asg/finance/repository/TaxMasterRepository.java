package com.asg.finance.repository;

import com.asg.finance.entity.TaxMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TaxMasterRepository extends JpaRepository<TaxMaster, Long> {
    Optional<TaxMaster> findByTaxPoid(Long taxPoid);
    List<TaxMaster> findByTaxPoidIn(Set<Long> taxPoids);
    boolean existsByTaxCode(String taxCode);

    boolean existsByTaxCodeAndTaxPoidNot(String taxCode, Long taxPoid);

    boolean existsByTaxPoid(Long poid);
}
