package com.blinkit.review.client;

import com.blinkit.review.client.dto.HasOrderedResponse;
import com.blinkit.review.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", configuration = FeignConfig.class)
public interface OrderServiceClient {

    @GetMapping("/orders/internal/has-ordered")
    HasOrderedResponse hasOrdered(@RequestParam String userId, @RequestParam String productId);
}
