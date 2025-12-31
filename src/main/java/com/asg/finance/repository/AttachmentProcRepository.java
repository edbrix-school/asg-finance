/*
package com.asg.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AttachmentProcRepository {


        @PersistenceContext
        private EntityManager em;

        @SuppressWarnings("unchecked")
        public List<Object[]> fetchActiveAttachments(Long groupPoid, Long companyPoid,
                                                     String docId, Long docKeyPoid) {
            StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_ATTACHMENTS_LOADLIST");

            query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

            query.setParameter("P_GROUP_POID", groupPoid);
            query.setParameter("P_COMPANY_POID", companyPoid);
            query.setParameter("P_DOC_ID", docId);
            query.setParameter("P_DOC_KEY_POID", docKeyPoid);

            return query.getResultList();
        }

}
*/
