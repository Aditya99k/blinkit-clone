package com.blinkit.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignPartnerRequest {

    @NotBlank(message = "partnerId is required")
    private String partnerId;
}
