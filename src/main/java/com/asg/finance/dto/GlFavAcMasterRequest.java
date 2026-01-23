package com.asg.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating/updating Key Favorite Account Master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlFavAcMasterRequest {

    @NotBlank(message = "Fav Ac Code is mandatory")
    @Size(max = 20, message = "Fav Ac Code must be at most 20 characters")
    private String favAcCode;

    @NotBlank(message = "Description is mandatory")
    @Size(max = 100, message = "Description must be at most 100 characters")
    private String description;

    @Size(max = 100, message = "Description2 must be at most 100 characters")
    private String description2;

    private Integer seqNo;

    @Size(max = 1, message = "Active must be at most 1 character")
    @Builder.Default
    private String active = "Y";

    /**
     */
    @Valid
    private List<UserRoleDetailRequest> userRoles;

    /**
     * List of GL account details with company and view category mappings
     */
    @NotEmpty(message = "At least one GL account must be mapped")
    @Valid
    private List<GlAccountDetailRequest> glAccounts;
}

