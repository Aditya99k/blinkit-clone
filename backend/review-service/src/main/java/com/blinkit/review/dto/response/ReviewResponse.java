package com.blinkit.review.dto.response;

import com.blinkit.review.entity.Review;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReviewResponse {

    private String reviewId;
    private String userId;
    private String productId;
    private String productName;
    private int rating;
    private String title;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;

    public static ReviewResponse from(Review r) {
        return ReviewResponse.builder()
                .reviewId(r.getReviewId())
                .userId(r.getUserId())
                .productId(r.getProductId())
                .productName(r.getProductName())
                .rating(r.getRating())
                .title(r.getTitle())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
