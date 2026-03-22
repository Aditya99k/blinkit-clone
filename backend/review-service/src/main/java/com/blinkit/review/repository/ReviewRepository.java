package com.blinkit.review.repository;

import com.blinkit.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends MongoRepository<Review, String> {

    Optional<Review> findByReviewId(String reviewId);

    Optional<Review> findByUserIdAndProductId(String userId, String productId);

    Page<Review> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);

    List<Review> findByUserId(String userId);

    boolean existsByUserIdAndProductId(String userId, String productId);
}
