package com.blinkit.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterPartnerRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String phone;

    @NotBlank(message = "Vehicle type is required")
    @Pattern(regexp = "^(BICYCLE|MOTORCYCLE|SCOOTER|CAR)$", message = "vehicleType must be BICYCLE, MOTORCYCLE, SCOOTER, or CAR")
    private String vehicleType;

    private String vehicleNumber;
}
