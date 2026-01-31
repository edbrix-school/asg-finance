package com.asg.finance.repository;

import com.asg.finance.entity.GlFavAcMasterUserRoleDtl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for GL_FAV_AC_MASTER_USER_ROLE_DTL table
 * Repository for user role detail records
 */
@Repository
public interface GlFavAcMasterUserRoleDtlRepository extends JpaRepository<GlFavAcMasterUserRoleDtl, GlFavAcMasterUserRoleDtl.CompositeKey> {
    
    List<GlFavAcMasterUserRoleDtl> findByFavAcPoid(Long favAcPoid);

    @Query("SELECT u FROM GlFavAcMasterUserRoleDtl u WHERE u.favAcPoid = :favAcPoid ORDER BY u.detRowId ASC")
    List<GlFavAcMasterUserRoleDtl> findByFavAcPoidOrderByDetRowId(@Param("favAcPoid") Long favAcPoid);
    
    @Modifying
    @Query("DELETE FROM GlFavAcMasterUserRoleDtl u WHERE u.favAcPoid = :favAcPoid")
    void deleteByFavAcPoid(@Param("favAcPoid") Long favAcPoid);

    List<GlFavAcMasterUserRoleDtl> findByUserRolePoid(Long userRolePoid);
    
    @Query("SELECT u FROM GlFavAcMasterUserRoleDtl u WHERE u.favAcPoid = :favAcPoid AND u.detRowId = :detRowId")
    java.util.Optional<GlFavAcMasterUserRoleDtl> findByFavAcPoidAndDetRowId(@Param("favAcPoid") Long favAcPoid, @Param("detRowId") Long detRowId);
}

