package com.blinkit.delivery.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusUpdatedEvent {
    private String taskId;
    private String orderId;
    private String deliveryPartnerId;
    private String deliveryStatus;  // ASSIGNED, PICKED_UP, OUT_FOR_DELIVERY, DELIVERED, FAILED, CANCELLED
    private Instant updatedAt;
}
