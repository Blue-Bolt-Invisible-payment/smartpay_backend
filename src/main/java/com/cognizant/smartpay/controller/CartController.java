package com.cognizant.smartpay.controller;

import com.cognizant.smartpay.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for cart operations
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Get cart items for a user
     * GET /api/cart/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId) {
        List<Map<String, Object>> cartItems = cartService.getCartItems(userId);
        return ResponseEntity.ok(cartItems);
    }

    /**
     * Get cart total for a user
     * GET /api/cart/{userId}/total
     */
    @GetMapping("/{userId}/total")
    public ResponseEntity<?> getCartTotal(@PathVariable Long userId) {
        Map<String, Object> total = cartService.getCartTotal(userId);
        return ResponseEntity.ok(total);
    }

    /**
     * Update cart item quantity
     * PUT /api/cart/{userId}/item/{cartItemId}
     */
    @PutMapping("/{userId}/item/{cartItemId}")
    public ResponseEntity<?> updateCartItem(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            @RequestBody Map<String, Integer> request) {
        int quantity = request.get("quantity");
        cartService.updateCartItemQuantity(cartItemId, quantity);
        return ResponseEntity.ok(Map.of("success", true, "message", "Cart item updated"));
    }

    /**
     * Remove item from cart
     * DELETE /api/cart/{userId}/item/{cartItemId}
     */
    @DeleteMapping("/{userId}/item/{cartItemId}")
    public ResponseEntity<?> removeCartItem(
            @PathVariable Long userId,
            @PathVariable Long cartItemId) {
        cartService.removeCartItem(cartItemId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Item removed from cart"));
    }

    /**
     * Clear cart for a user (after payment)
     * DELETE /api/cart/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of("message", "Cart cleared successfully"));
    }

    /**
     * Add item to cart by RFID tag
     * POST /api/cart/add-by-rfid
     */
    @PostMapping("/add-by-rfid")
    public ResponseEntity<?> addItemByRfid(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String rfidTag = request.get("rfidTag").toString();

        cartService.addItemByRfid(userId, rfidTag);
        return ResponseEntity.ok(Map.of("success", true, "message", "Item added to cart"));
    }

    @GetMapping("/health")
    public String testApp(){

        return "Application is up and running fine";
    }
}
