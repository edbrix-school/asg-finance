package com.asg.finance.repository;

import com.asg.finance.entity.GlobalCurrencyMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlobalCurrencyMasterRepository extends JpaRepository<GlobalCurrencyMaster, Long> {
    
    Optional<GlobalCurrencyMaster> findByCurrencyCode(String currencyCode);
    
    Optional<GlobalCurrencyMaster> findByCurrencyName(String currencyName);
    
    List<GlobalCurrencyMaster> findByActiveAndDeleted(String active, String deleted);
    
    boolean existsByCurrencyCodeIgnoreCase(String currencyCode);
    
    boolean existsByCurrencyNameIgnoreCase(String currencyName);
}