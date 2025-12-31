package com.asg.finance.dto;

public record AddressMasterLightDto(
        Long addressMasterPoid,
        String addressName,
        String countryName,
        String label,
        Long value,
        String active
) {
    public AddressMasterLightDto {
        // Normalize strings to uppercase
        if (addressName != null) {
            addressName = addressName.toUpperCase();
        }
        if (label != null) {
            label = label.toUpperCase();
        }
    }
}

