package com.asg.finance.dto;

import com.asg.finance.enums.AssetDisposalProcess;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetDisposalDto {

    private Long assetPoid;
    private AssetDisposalProcess process;
    private Double scrapOrSoldValue;
    private String remarks;
}


