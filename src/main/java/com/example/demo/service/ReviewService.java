package com.example.demo.service;

import com.example.demo.dto.request.ReviewRequest;
import com.example.demo.dto.response.ReviewResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ReviewService {

    ReviewResponse addReview(String username, Long productId, ReviewRequest request);

    List<ReviewResponse> getReviewsByProduct(Long productId);

    void deleteReview(Long reviewId, String username);

    Page<ReviewResponse> getAllReviewsForAdmin(Pageable pageable);

    void deleteReviewByAdmin(Long reviewId);
}
