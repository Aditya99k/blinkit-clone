package com.blinkit.delivery.kafka;

import com.blinkit.delivery.event.DeliveryStatusUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventPublisher {

    public static final String TOPIC_DELIVERY_STATUS_UPDATED = "delivery.status.updated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishStatusUpdated(DeliveryStatusUpdatedEvent event) {
        kafkaTemplate.send(TOPIC_DELIVERY_STATUS_UPDATED, event.getOrderId(), event);
        log.info("Published delivery.status.updated for orderId={} status={}",
                event.getOrderId(), event.getDeliveryStatus());
    }
}
