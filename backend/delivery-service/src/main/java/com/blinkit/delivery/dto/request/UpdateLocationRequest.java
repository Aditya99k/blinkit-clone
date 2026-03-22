package com.blinkit.delivery.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateLocationRequest {

    @NotNull(message = "lat is required")
    private Double lat;

    @NotNull(message = "lng is required")
    private Double lng;
}
