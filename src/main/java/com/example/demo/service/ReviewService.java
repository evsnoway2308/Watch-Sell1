package com.example.demo.service;

import com.example.demo.dto.request.ReviewRequest;
import com.example.demo.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {

    ReviewResponse addReview(String username, Long productId, ReviewRequest request);

    List<ReviewResponse> getReviewsByProduct(Long productId);

    void deleteReview(Long reviewId, String username);
}
