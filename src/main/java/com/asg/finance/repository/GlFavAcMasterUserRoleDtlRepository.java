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
public interface GlFavAcMasterUserRoleDtlRepository extends JpaRepository<GlFavAcMasterUserRoleDtl, Long> {
    
    List<GlFavAcMasterUserRoleDtl> findByFavAcPoid(Long favAcPoid);
    
    @Modifying
    @Query("DELETE FROM GlFavAcMasterUserRoleDtl u WHERE u.favAcPoid = :favAcPoid")
    void deleteByFavAcPoid(@Param("favAcPoid") Long favAcPoid);

    List<GlFavAcMasterUserRoleDtl> findByUserRolePoid(Long userRolePoid);
}

