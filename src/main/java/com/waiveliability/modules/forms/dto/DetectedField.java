package com.waiveliability.modules.forms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectedField {
    private String label;
    private String fieldType;
    private String placeholder;
    private boolean required;
    private int fieldOrder;
    private String content;
}
