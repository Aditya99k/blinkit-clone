package com.blinkit.delivery.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "delivery_partners")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPartner {

    @Id
    private String id;

    @Indexed(unique = true)
    private String partnerId;       // Same as AuthUser.userId (UUID)

    private String name;
    private String email;
    private String phone;

    private String vehicleType;     // BICYCLE, MOTORCYCLE, SCOOTER, CAR
    private String vehicleNumber;

    @Builder.Default
    private Boolean isAvailable = true;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Double avgRating = 5.0;

    @Builder.Default
    private Integer totalDeliveries = 0;

    private Double currentLat;
    private Double currentLng;
    private Instant lastLocationUpdatedAt;

    // Set after each delivery; partner stays unavailable until this time passes
    private Instant cooldownUntil;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
