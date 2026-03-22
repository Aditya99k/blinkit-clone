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
public class PaymentSuccessEvent {
    private String paymentId;
    private String orderId;
    private String userId;
    private String addressId;
    private double amount;
    private Instant paidAt;
}
