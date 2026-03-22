package com.blinkit.delivery.dto.response;

import com.blinkit.delivery.entity.DeliveryPartner;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DeliveryPartnerResponse {

    private String partnerId;
    private String name;
    private String email;
    private String phone;
    private String vehicleType;
    private String vehicleNumber;
    private Boolean isAvailable;
    private Boolean isActive;
    private Double avgRating;
    private Integer totalDeliveries;
    private Double currentLat;
    private Double currentLng;
    private Instant lastLocationUpdatedAt;
    private Instant createdAt;

    public static DeliveryPartnerResponse from(DeliveryPartner p) {
        return DeliveryPartnerResponse.builder()
                .partnerId(p.getPartnerId())
                .name(p.getName())
                .email(p.getEmail())
                .phone(p.getPhone())
                .vehicleType(p.getVehicleType())
                .vehicleNumber(p.getVehicleNumber())
                .isAvailable(p.getIsAvailable())
                .isActive(p.getIsActive())
                .avgRating(p.getAvgRating())
                .totalDeliveries(p.getTotalDeliveries())
                .currentLat(p.getCurrentLat())
                .currentLng(p.getCurrentLng())
                .lastLocationUpdatedAt(p.getLastLocationUpdatedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
