package com.blinkit.delivery.controller;

import com.blinkit.common.dto.ApiResponse;
import com.blinkit.delivery.dto.response.DeliveryTaskResponse;
import com.blinkit.delivery.service.DeliveryTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/delivery/track")
@RequiredArgsConstructor
public class TrackingController {

    private final DeliveryTaskService taskService;

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<DeliveryTaskResponse>> trackOrder(
            @PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.ok("Tracking info fetched", taskService.trackByOrderId(orderId)));
    }
}
