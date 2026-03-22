package com.blinkit.order.controller;

import com.blinkit.common.dto.ApiResponse;
import com.blinkit.common.enums.OrderStatus;
import com.blinkit.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/orders/internal")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderRepository orderRepository;

    /**
     * Called by review-service (via Feign + X-Internal-Secret) to verify
     * whether a user has at least one DELIVERED order containing a given product.
     */
    @GetMapping("/has-ordered")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> hasOrdered(
            @RequestParam String userId,
            @RequestParam String productId) {
        boolean hasOrdered = orderRepository
                .existsByUserIdAndStatusAndItemsProductId(userId, OrderStatus.DELIVERED, productId);
        return ResponseEntity.ok(ApiResponse.ok("Check complete", Map.of("hasOrdered", hasOrdered)));
    }
}
