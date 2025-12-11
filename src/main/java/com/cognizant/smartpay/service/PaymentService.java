package com.cognizant.smartpay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for payment processing
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Process payment for user's cart
     */
    @Transactional
    public Map<String, Object> processPayment(Long userId) {
        log.info("Processing payment for user: {}", userId);

        // Get user's cart
        String cartSql = """
            SELECT c.cart_id, ci.cart_item_id, ci.product_id, ci.quantity, ci.subtotal, p.selling_price, p.name, p.brand
            FROM cart c
            JOIN cart_items ci ON c.cart_id = ci.cart_id
            JOIN products p ON ci.product_id = p.product_id
            WHERE c.user_id = ? AND c.is_active = 1
            """;
        List<Map<String, Object>> cartItems = jdbcTemplate.query(cartSql, (rs, rowNum) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("cartId", rs.getLong("cart_id"));
            item.put("cartItemId", rs.getLong("cart_item_id"));
            item.put("productId", rs.getLong("product_id"));
            item.put("quantity", rs.getInt("quantity"));
            item.put("subtotal", rs.getBigDecimal("subtotal"));
            item.put("unitPrice", rs.getBigDecimal("selling_price"));
            item.put("productName", rs.getString("name"));
            item.put("productBrand", rs.getString("brand"));
            return item;
        }, userId);

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // Calculate total
        BigDecimal totalAmount = cartItems.stream()
            .map(item -> (BigDecimal) item.get("subtotal"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Check wallet balance
        String walletSql = "SELECT balance FROM wallet WHERE user_id = ?";
        BigDecimal walletBalance = jdbcTemplate.queryForObject(walletSql, BigDecimal.class, userId);

        if (walletBalance.compareTo(totalAmount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        // Get cart ID for transaction
        Long cartId = (Long) cartItems.get(0).get("cartId");

        // Generate transaction reference
        String transactionReference = "TXN" + System.currentTimeMillis();

        // Create transaction record
        String transactionSql = """
            INSERT INTO transactions (transaction_reference, user_id, cart_id, total_amount, final_amount, payment_method, payment_status, wallet_balance_before, wallet_balance_after, items_count, transaction_date)
            VALUES (?, ?, ?, ?, ?, 'WALLET', 'SUCCESS', ?, ?, ?, NOW())
            """;
        jdbcTemplate.update(transactionSql,
            transactionReference,
            userId,
            cartId,
            totalAmount,
            totalAmount,
            walletBalance,
            walletBalance.subtract(totalAmount),
            cartItems.size());

        // Get the transaction ID that was just inserted
        String getTransactionIdSql = "SELECT transaction_id FROM transactions WHERE transaction_reference = ?";
        Long transactionId = jdbcTemplate.queryForObject(getTransactionIdSql, Long.class, transactionReference);

        // Create transaction items
        for (Map<String, Object> item : cartItems) {
            String itemSql = """
                INSERT INTO transaction_items (transaction_id, product_id, product_name, product_brand, quantity, unit_price, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
            jdbcTemplate.update(itemSql,
                transactionId,
                item.get("productId"),
                item.get("productName"),
                item.get("productBrand"),
                item.get("quantity"),
                item.get("unitPrice"),
                item.get("subtotal"));
        }

        // Update wallet balance
        String updateWalletSql = "UPDATE wallet SET balance = balance - ? WHERE user_id = ?";
        jdbcTemplate.update(updateWalletSql, totalAmount, userId);

        // Update product stock
        for (Map<String, Object> item : cartItems) {
            String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";
            jdbcTemplate.update(updateStockSql, item.get("quantity"), item.get("productId"));
        }

        // Clear cart
        String clearCartSql = "DELETE FROM cart_items WHERE cart_id = ?";
        jdbcTemplate.update(clearCartSql, cartId);

        String deactivateCartSql = "UPDATE cart SET is_active = 0 WHERE cart_id = ?";
        jdbcTemplate.update(deactivateCartSql, cartId);

        // Get new wallet balance
        BigDecimal newBalance = jdbcTemplate.queryForObject(walletSql, BigDecimal.class, userId);

        log.info("Payment processed successfully for user: {}, transaction: {}, amount: {}",
            userId, transactionReference, totalAmount);

        // Return payment result
        Map<String, Object> result = new HashMap<>();
        result.put("transactionId", transactionReference);
        result.put("status", "success");
        result.put("receipt", "Payment receipt generated");
        result.put("newBalance", newBalance);
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("amount", totalAmount);

        return result;
    }
}