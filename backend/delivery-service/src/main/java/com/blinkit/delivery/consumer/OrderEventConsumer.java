package com.blinkit.delivery.consumer;

import com.blinkit.delivery.event.OrderCancelledEvent;
import com.blinkit.delivery.event.PaymentSuccessEvent;
import com.blinkit.delivery.service.DeliveryTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final DeliveryTaskService taskService;

    /**
     * payment.success — payment service publishes this after wallet debit succeeds.
     * Delivery service now reacts directly to payment, no longer depends on order.confirmed.
     */
    @KafkaListener(
            topics = "payment.success",
            groupId = "delivery-service",
            containerFactory = "paymentSuccessKafkaListenerContainerFactory"
    )
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        log.info("Received payment.success for orderId={} userId={}", event.getOrderId(), event.getUserId());
        try {
            taskService.createTask(event.getOrderId(), event.getUserId(), event.getAddressId());
        } catch (Exception e) {
            log.error("Failed to create delivery task for orderId={}: {}", event.getOrderId(), e.getMessage());
        }
    }

    /**
     * order.cancelled — order-service publishes this when a customer cancels.
     * Cancel the delivery task if it hasn't already been picked up.
     */
    @KafkaListener(
            topics = "order.cancelled",
            groupId = "delivery-service",
            containerFactory = "orderCancelledKafkaListenerContainerFactory"
    )
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Received order.cancelled for orderId={}", event.getOrderId());
        try {
            taskService.cancelTask(event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to cancel delivery task for orderId={}: {}", event.getOrderId(), e.getMessage());
        }
    }
}
