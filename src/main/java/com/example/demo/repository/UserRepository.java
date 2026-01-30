package com.example.demo.repository;
import com.example.demo.model.User;

public interface UserRepository extends org.springframework.data.jpa.repository.JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
