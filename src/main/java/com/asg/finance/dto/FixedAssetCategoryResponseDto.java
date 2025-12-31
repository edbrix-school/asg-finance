package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
import com.asg.common.lib.dto.UserRoleDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedAssetCategoryResponseDto {

    private Long faCategoryPoid;
    private String faCategoryCode;
    private String faCategoryDescription;
    private String faCategoryDescription2;
    private String assetType;
    private Long faGlAccount;
    private DetailsDto faGlAccountDet;
    private Long faAccumulationAccount;
    private DetailsDto faAccumulationAccountDet;
    private Long faDepreciationAccount;
    private DetailsDto faDepreciationAccountDet;
    private Long costCenter;
    private DetailsDto costCenterDet;
    private List<String> userRolePoid;
    private List<UserRoleDto> userRolesPoidDet;
    private Integer seqNo;
    private String active;
}
