package com.asg.finance.service;


import com.asg.common.lib.dto.request.DocReleaseLockRequestDto;
import com.asg.finance.dto.GlLedgerDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;

@Slf4j
@Service
public class GLMasterCustomServiceImpl implements GLMasterCustomService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;


    @Override
    public GlLedgerDTO callProcGlMasterCreate(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            String loginUser,
            String code,
            String description,
            String glType) {

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_GL_MASTER_CREATE");

        // Input parameters
        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_CODE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DESC", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_GL_TYPE", String.class, ParameterMode.IN);

        // Output parameters
        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("P_NEW_GL_POID", Long.class, ParameterMode.OUT);

        // Set input parameter values
        query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
        query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
        query.setParameter("P_LOGIN_USER", loginUser);
        query.setParameter("P_CODE", code);
        query.setParameter("P_DESC", description);
        query.setParameter("P_GL_TYPE", glType);

        // Execute the procedure
        query.execute();

        // Get output parameters
        String status = (String) query.getOutputParameterValue("P_STATUS");
        Long newGlPoid = (Long) query.getOutputParameterValue("P_NEW_GL_POID");

        // Create response based on status
        GlLedgerDTO response = new GlLedgerDTO();

        return response;
    }

    @Override
    public String acquireLock(DocReleaseLockRequestDto request)  {
        String status = null;

        String sql = "{ call PROC_GLOB_DOC_ACQUIRE_LOCK(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection connection = dataSource.getConnection();
             CallableStatement cs = connection.prepareCall(sql)) {

            cs.setLong(1, request.getLoginGroupPoid());
            cs.setLong(2, request.getLoginCompanyPoid());
            cs.setLong(3, Long.parseLong(request.getLoginUserPoid()));
            cs.setString(4, request.getDocId());

            cs.setLong(5, request.getDocPoidValue());

            cs.setString(6, request.getUserId());

            cs.registerOutParameter(7, java.sql.Types.VARCHAR);

            cs.execute();

            status = cs.getString(7);
        }
        catch (Exception e) {
            log.error(" error : {}", e.getMessage());
        }
        return status;
    }

    @Override
    public String releaseLock(DocReleaseLockRequestDto request)  {
        String status = null;

        String sql = "{ call PROC_GLOB_DOC_RELEASE_LOCK(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection connection = dataSource.getConnection();
             CallableStatement cs = connection.prepareCall(sql)) {

            cs.setLong(1, request.getLoginGroupPoid());
            cs.setLong(2, request.getLoginCompanyPoid());
            cs.setLong(3, Long.parseLong(request.getLoginUserPoid()));
            cs.setString(4, request.getDocId());

            cs.setLong(5, request.getDocPoidValue());

            cs.setString(6, request.getUserId());

            cs.registerOutParameter(7, java.sql.Types.VARCHAR);

            cs.execute();

            status = cs.getString(7);
        }
        catch (Exception e) {
            log.error(" error : {}", e.getMessage());
        }
        return status;
    }
}
