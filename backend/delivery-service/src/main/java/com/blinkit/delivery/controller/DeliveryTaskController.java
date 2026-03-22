package com.blinkit.delivery.controller;

import com.blinkit.common.dto.ApiResponse;
import com.blinkit.delivery.dto.request.UpdateTaskStatusRequest;
import com.blinkit.delivery.dto.response.DeliveryTaskResponse;
import com.blinkit.delivery.service.DeliveryTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/delivery/tasks")
@RequiredArgsConstructor
public class DeliveryTaskController {

    private final DeliveryTaskService taskService;

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<DeliveryTaskResponse>>> getMyTasks(
            @RequestHeader("X-User-Id") String partnerId,
            @RequestHeader("X-User-Role") String role) {
        requireDeliveryAgent(role);
        return ResponseEntity.ok(ApiResponse.ok("Tasks fetched", taskService.getMyTasks(partnerId)));
    }

    @PutMapping("/{taskId}/status")
    public ResponseEntity<ApiResponse<DeliveryTaskResponse>> updateStatus(
            @RequestHeader("X-User-Id") String partnerId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskStatusRequest req) {
        requireDeliveryAgent(role);
        return ResponseEntity.ok(ApiResponse.ok("Task status updated",
                taskService.updateTaskStatus(taskId, partnerId, req)));
    }

    private void requireDeliveryAgent(String role) {
        if (!"DELIVERY_AGENT".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Delivery agent access required");
        }
    }
}
