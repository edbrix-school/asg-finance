package com.asg.finance.repository.master;

import com.asg.finance.entity.master.ShipChargeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ShipChargeRepository extends JpaRepository<ShipChargeEntity, Long> {
    Optional<ShipChargeEntity> findByChargePoid(Long chargePoid);
    List<ShipChargeEntity> findByChargePoidIn(Set<Long> chargePoids);
    boolean existsByChargePoid(Long chargePoid);
}
