package com.asg.finance.repository;

import com.asg.finance.entity.ShipPrincipalMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipPrincipalMasterRepository extends JpaRepository<ShipPrincipalMaster, Long> {

    ShipPrincipalMaster findByPrincipalPoid(Long principalPoid);
    
    boolean existsByPrincipalPoid(Long poid);
}

