package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Key Favorite Account Master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlFavAcMasterResponse {

    private Long favAcPoid;
    private Long groupPoid;
    private String favAcCode;
    private String description;
    private String description2;
    private String active;
    private Integer seqNo;
    private String deleted;
    private List<GlAccountDetailResponse> glAccounts;
    private List<UserRoleDetailResponse> userRoles;

}

