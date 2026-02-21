package com.waiveliability.modules.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitsResponse {
    private LimitInfo forms;
    private LimitInfo submissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LimitInfo {
        private int used;
        private int limit;
    }
}
