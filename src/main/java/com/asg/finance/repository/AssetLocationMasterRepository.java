package com.asg.finance.repository;

import com.asg.finance.entity.AssetLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetLocationMasterRepository extends JpaRepository<AssetLocation, Long> {

    Optional<AssetLocation> findByLocationPoid(Long locationPoid);
    boolean existsByLocationCode( String locationCode);
    boolean existsByDescription( String description);
    boolean existsByLocationCodeAndLocationPoidNot(String locationCode, Long locationPoid);
    boolean existsByDescriptionAndLocationPoidNot(String description, Long locationPoid);

}