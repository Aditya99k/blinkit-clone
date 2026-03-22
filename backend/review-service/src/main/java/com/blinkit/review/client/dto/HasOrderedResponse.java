package com.blinkit.review.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class HasOrderedResponse {
    private boolean success;
    private String message;
    private Map<String, Boolean> data;

    public boolean hasOrdered() {
        return data != null && Boolean.TRUE.equals(data.get("hasOrdered"));
    }
}
