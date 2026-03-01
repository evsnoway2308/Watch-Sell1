package com.example.demo.controller;

import com.example.demo.dto.response.CartResponse;
import com.example.demo.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    @PostMapping("/add")
    public ResponseEntity<String> addToCart(@RequestParam Long productId, @RequestParam Integer quantity) {
        cartService.addToCart(productId, quantity);
        return ResponseEntity.ok("Item added to cart");
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateQuantity(@RequestParam Long productId, @RequestParam Integer quantity) {
        cartService.updateQuantity(productId, quantity);
        return ResponseEntity.ok("Cart updated");
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<String> removeFromCart(@PathVariable Long productId) {
        cartService.removeFromCart(productId);
        return ResponseEntity.ok("Item removed from cart");
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok("Cart cleared");
    }
}
