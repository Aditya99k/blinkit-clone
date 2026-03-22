package com.blinkit.delivery.controller;

import com.blinkit.common.dto.ApiResponse;
import com.blinkit.delivery.dto.request.RegisterPartnerRequest;
import com.blinkit.delivery.dto.request.UpdateLocationRequest;
import com.blinkit.delivery.dto.response.DeliveryPartnerResponse;
import com.blinkit.delivery.service.DeliveryPartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/delivery/partners")
@RequiredArgsConstructor
public class DeliveryPartnerController {

    private final DeliveryPartnerService partnerService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> register(
            @RequestHeader("X-User-Id") String partnerId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @Valid @RequestBody RegisterPartnerRequest req) {
        requireDeliveryAgent(role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Delivery partner registered", partnerService.register(partnerId, email, req)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> getMyProfile(
            @RequestHeader("X-User-Id") String partnerId,
            @RequestHeader("X-User-Role") String role) {
        requireDeliveryAgent(role);
        return ResponseEntity.ok(ApiResponse.ok("Profile fetched", partnerService.getMyProfile(partnerId)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> updateProfile(
            @RequestHeader("X-User-Id") String partnerId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody RegisterPartnerRequest req) {
        requireDeliveryAgent(role);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", partnerService.updateProfile(partnerId, req)));
    }

    @PutMapping("/me/availability")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> setAvailability(
            @RequestHeader("X-User-Id") String partnerId,
            @RequestHeader("X-User-Role") String role,
            @RequestBody Map<String, Boolean> body) {
        requireDeliveryAgent(role);
        Boolean available = body.get("available");
        if (available == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "available field is required");
        }
        return ResponseEntity.ok(ApiResponse.ok("Availability updated", partnerService.setAvailability(partnerId, available)));
    }

    @PutMapping("/me/location")
    public ResponseEntity<ApiResponse<DeliveryPartnerResponse>> updateLocation(
            @RequestHeader("X-User-Id") String partnerId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody UpdateLocationRequest req) {
        requireDeliveryAgent(role);
        return ResponseEntity.ok(ApiResponse.ok("Location updated", partnerService.updateLocation(partnerId, req)));
    }

    private void requireDeliveryAgent(String role) {
        if (!"DELIVERY_AGENT".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Delivery agent access required");
        }
    }
}
