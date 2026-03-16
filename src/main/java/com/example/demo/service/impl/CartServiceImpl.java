package com.example.demo.service.impl;

import com.example.demo.dto.response.CartItemResponse;
import com.example.demo.dto.response.CartResponse;
import com.example.demo.model.Cart;
import com.example.demo.model.CartItem;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CartResponse getCart() {
        Cart cart = getOrCreateCart();

        List<CartItem> itemsToRemove = cart.getItems().stream()
                .filter(item -> Boolean.TRUE.equals(item.getProduct().getIsDeleted()))
                .collect(Collectors.toList());

        if (!itemsToRemove.isEmpty()) {
            cart.getItems().removeAll(itemsToRemove);
            cartItemRepository.deleteAll(itemsToRemove);
            cart = cartRepository.save(cart);
        }

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public void addToCart(Long productId, Integer quantity) {
        Cart cart = getOrCreateCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElse(new CartItem(null, 0, cart, product));

        if (cartItem.getQuantity() + quantity > product.getStock()) {
            throw new RuntimeException("Số lượng yêu cầu vượt quá số lượng hàng trong kho");
        }

        cartItem.setQuantity(cartItem.getQuantity() + quantity);
        cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public void updateQuantity(Long productId, Integer quantity) {
        Cart cart = getOrCreateCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new RuntimeException("Item not in cart"));

        if (quantity > product.getStock()) {
            throw new RuntimeException("Số lượng yêu cầu vượt quá số lượng hàng trong kho");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }
    }

    @Override
    @Transactional
    public void removeFromCart(Long productId) {
        Cart cart = getOrCreateCart();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new RuntimeException("Item not in cart"));

        cartItemRepository.delete(cartItem);
    }

    @Override
    @Transactional
    public void clearCart() {
        Cart cart = getOrCreateCart();
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());

        double totalAmount = items.stream()
                .mapToDouble(CartItemResponse::getSubTotal)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .totalAmount(totalAmount)
                .build();
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        Product product = item.getProduct();
        double subTotal = product.getPrice() * item.getQuantity();

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productPrice(product.getPrice())
                .productImageUrl(product.getImageUrl())
                .quantity(item.getQuantity())
                .productStock(product.getStock())
                .subTotal(subTotal)
                .build();
    }
}
