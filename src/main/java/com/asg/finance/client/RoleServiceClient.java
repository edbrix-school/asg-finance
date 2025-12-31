package com.asg.finance.client;

import com.asg.common.lib.client.GenericRestClient;
import com.asg.common.lib.dto.CompanyDto;
import com.asg.common.lib.dto.RoleDto;
import com.asg.common.lib.dto.response.ApiResponseWrapper;
import com.asg.common.lib.utility.RestClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RoleServiceClient {
    
    private final GenericRestClient restClient;
    
    @Value("${settings.service.url:http://localhost:8084/setting/api}")
    private String settingsServiceUrl;
    
    public RoleDto findById(Long userRolePoid) {
        String url = settingsServiceUrl + "/v1/user-roles/simple/" + userRolePoid;
        ApiResponseWrapper<RoleDto> response = restClient.get(url, new ParameterizedTypeReference<ApiResponseWrapper<RoleDto>>() {});
        return RestClientUtil.extractData(response);
    }
    
    public List<RoleDto> findByUserRolePoidIn(List<Long> userRolePoids) {
        String url = settingsServiceUrl + "/v1/user-roles/batch";
        ApiResponseWrapper<List<RoleDto>> response = restClient.post(url, userRolePoids, new ParameterizedTypeReference<ApiResponseWrapper<List<RoleDto>>>() {});
        return RestClientUtil.extractListData(response);
    }

}
