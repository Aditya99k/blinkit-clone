package com.blinkit.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductRatingSummary {
    private String productId;
    private double averageRating;
    private long totalReviews;
}
