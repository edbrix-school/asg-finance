//package com.asg.repository.master;
//
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import com.asg.dto.masters.UserRoleDto;
//import jakarta.persistence.TypedQuery;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//public class RoleCustomRepositoryImpl implements RoleCustomRepository {
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//    @Override
//    public Page<UserRoleDto> findUserRolesWithPaginationAndSorting(String userRoleId, String userRoleName, Pageable pageable) {
//
//        StringBuilder queryStr = new StringBuilder(
//                "SELECT new com.asg.dto.masters.UserRoleDto(" +
//                        "r.userRolePoid, r.userRoleId, r.userRoleName, r.active) " +
//                        "FROM RoleEntity r " +
//                        "WHERE (:userRoleId IS NULL OR LOWER(r.userRoleId) LIKE LOWER(CONCAT('%', :userRoleId, '%'))) " +
//                        "AND (:userRoleName IS NULL OR LOWER(r.userRoleName) LIKE LOWER(CONCAT('%', :userRoleName, '%'))) " +
//                        "AND r.active = 'Y' " +
//                        "AND (r.deleted IS NULL OR r.deleted = 'N') "
//        );
//
//        if (pageable.getSort().isSorted()) {
//            String orderBy = pageable.getSort().stream()
//                    .map(order -> {
//                        String property = order.getProperty();
//                        if ("userRoleName".equals(property)) {
//                            return "LOWER(r." + property + ") " + order.getDirection().name();
//                        }
//                        return "r." + property + " " + order.getDirection().name();
//                    })
//                    .collect(Collectors.joining(", "));
//            queryStr.append(" ORDER BY ").append(orderBy);
//        }
//
//        TypedQuery<UserRoleDto> query = entityManager.createQuery(queryStr.toString(), UserRoleDto.class);
//        query.setParameter("userRoleId", userRoleId);
//        query.setParameter("userRoleName", userRoleName);
//
//        query.setFirstResult((int) pageable.getOffset());
//        query.setMaxResults(pageable.getPageSize());
//
//        String countQueryStr =
//                "SELECT COUNT(r) FROM RoleEntity r " +
//                        "WHERE (:userRoleId IS NULL OR LOWER(r.userRoleId) LIKE LOWER(CONCAT('%', :userRoleId, '%'))) " +
//                        "AND (:userRoleName IS NULL OR LOWER(r.userRoleName) LIKE LOWER(CONCAT('%', :userRoleName, '%'))) " +
//                        "AND r.active = 'Y' " +
//                        "AND (r.deleted IS NULL OR r.deleted = 'N')";
//
//        TypedQuery<Long> countQuery = entityManager.createQuery(countQueryStr, Long.class);
//        countQuery.setParameter("userRoleId", userRoleId);
//        countQuery.setParameter("userRoleName", userRoleName);
//
//        long total = countQuery.getSingleResult();
//        List<UserRoleDto> content = query.getResultList();
//
//        return new PageImpl<>(content, pageable, total);
//    }
//}
//
