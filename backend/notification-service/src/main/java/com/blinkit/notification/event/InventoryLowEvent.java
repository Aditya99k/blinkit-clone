package com.blinkit.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLowEvent {
    private String productId;
    private String productName;
    private Integer availableQty;
    private Integer lowStockThreshold;
}
