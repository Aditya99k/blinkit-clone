package com.blinkit.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateTaskStatusRequest {

    @NotBlank(message = "status is required")
    @Pattern(regexp = "^(PICKED_UP|OUT_FOR_DELIVERY|DELIVERED|FAILED)$",
             message = "status must be PICKED_UP, OUT_FOR_DELIVERY, DELIVERED, or FAILED")
    private String status;

    private String failureReason;  // required when status = FAILED
}
