package com.example.demo.repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import com.example.demo.model.User;

public interface UserRepository extends org.springframework.data.jpa.repository.JpaRepository<User, Long> , JpaSpecificationExecutor<User> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    User findByUsername(String username);
      Optional<User> findByEmail(String email);
}
