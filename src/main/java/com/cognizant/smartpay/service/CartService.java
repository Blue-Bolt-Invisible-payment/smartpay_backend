package com.cognizant.smartpay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for cart operations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get cart items for a user with product details
     */
    public List<Map<String, Object>> getCartItems(Long userId) {
        log.debug("Fetching cart items for user: {}", userId);
        
        String sql = """
            SELECT 
                ci.cart_item_id as id,
                ci.quantity,
                p.product_id as productId,
                p.name as productName,
                p.brand,
                p.category_id as categoryId,
                p.selling_price as price,
                p.mrp,
                p.unit,
                p.image_url as imageUrl,
                ci.subtotal
            FROM cart c
            JOIN cart_items ci ON c.cart_id = ci.cart_id
            JOIN products p ON ci.product_id = p.product_id
            WHERE c.user_id = ? AND c.is_active = 1
            ORDER BY ci.added_at DESC
            """;
        
        List<Map<String, Object>> items = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("userId", userId);
            item.put("quantity", rs.getInt("quantity"));
            
            // Product details nested in 'item' object for frontend compatibility
            Map<String, Object> product = new HashMap<>();
            product.put("id", rs.getLong("productId"));
            product.put("name", rs.getString("productName"));
            product.put("brand", rs.getString("brand"));
            product.put("category", rs.getString("categoryId"));
            product.put("price", rs.getBigDecimal("price"));
            product.put("mrp", rs.getBigDecimal("mrp"));
            product.put("unit", rs.getString("unit"));
            product.put("imageUrl", rs.getString("imageUrl"));
            
            item.put("item", product);
            return item;
        }, userId);
        
        log.info("Found {} cart items for user {}", items.size(), userId);
        return items;
    }

    /**
     * Get cart total with breakdown
     */
    public Map<String, Object> getCartTotal(Long userId) {
        log.debug("Calculating cart total for user: {}", userId);
        
        String sql = """
            SELECT 
                COALESCE(SUM(ci.subtotal), 0) as subtotal,
                COUNT(ci.cart_item_id) as itemCount
            FROM cart c
            JOIN cart_items ci ON c.cart_id = ci.cart_id
            WHERE c.user_id = ? AND c.is_active = 1
            """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            BigDecimal subtotal = rs.getBigDecimal("subtotal");
            int itemCount = rs.getInt("itemCount");
            
            // Prices are inclusive of tax - no additional tax calculation
            BigDecimal tax = BigDecimal.ZERO;
            BigDecimal total = subtotal;
            
            Map<String, Object> result = new HashMap<>();
            result.put("subtotal", subtotal);
            result.put("discount", BigDecimal.ZERO);
            result.put("tax", tax);
            result.put("total", total);
            result.put("itemCount", itemCount);
            
            return result;
        }, userId);
    }

    /**
     * Clear cart after payment
     */
    @Transactional
    public void clearCart(Long userId) {
        log.debug("Clearing cart for user: {}", userId);
        
        // Delete cart items
        String deleteItemsSql = """
            DELETE ci FROM cart_items ci
            JOIN cart c ON ci.cart_id = c.cart_id
            WHERE c.user_id = ? AND c.is_active = 1
            """;
        
        int deletedItems = jdbcTemplate.update(deleteItemsSql, userId);
        
        // Mark cart as inactive
        String updateCartSql = """
            UPDATE cart 
            SET is_active = 0 
            WHERE user_id = ? AND is_active = 1
            """;
        
        jdbcTemplate.update(updateCartSql, userId);
        
        log.info("Cleared {} items from cart for user {}", deletedItems, userId);
    }

    /**
     * Update cart item quantity and recalculate subtotal
     */
    @Transactional
    public void updateCartItemQuantity(Long cartItemId, int quantity) {
        log.debug("Updating cart item {} to quantity {}", cartItemId, quantity);
        
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        
        // Get the product price and calculate new subtotal
        String updateSql = """
            UPDATE cart_items ci
            JOIN products p ON ci.product_id = p.product_id
            SET ci.quantity = ?,
                ci.subtotal = p.selling_price * ?,
                ci.updated_at = NOW()
            WHERE ci.cart_item_id = ?
            """;
        
        int updated = jdbcTemplate.update(updateSql, quantity, quantity, cartItemId);
        
        if (updated == 0) {
            throw new IllegalArgumentException("Cart item not found: " + cartItemId);
        }
        
        log.info("Updated cart item {} to quantity {}", cartItemId, quantity);
    }

    /**
     * Remove item from cart
     */
    @Transactional
    public void removeCartItem(Long cartItemId) {
        log.debug("Removing cart item: {}", cartItemId);
        
        String deleteSql = "DELETE FROM cart_items WHERE cart_item_id = ?";
        int deleted = jdbcTemplate.update(deleteSql, cartItemId);
        
        if (deleted == 0) {
            throw new IllegalArgumentException("Cart item not found: " + cartItemId);
        }
        
        log.info("Removed cart item: {}", cartItemId);
    }

    /**
     * Add item to cart by RFID tag
     */
    @Transactional
    public void addItemByRfid(Long userId, String rfidTag) {
        log.debug("Adding item by RFID tag: {} for user: {}", rfidTag, userId);
        
        // First, get product ID from RFID tag
        String productSql = "SELECT product_id FROM rfid_tags WHERE rfid_tag = ? AND is_active = 1";
        Long productId = jdbcTemplate.queryForObject(productSql, Long.class, rfidTag);
        
        if (productId == null) {
            throw new IllegalArgumentException("Product not found for RFID tag: " + rfidTag);
        }
        
        // Get or create cart for user - ensure only one active cart per user
        String getCartIdSql = "SELECT cart_id FROM cart WHERE user_id = ? AND is_active = 1";
        List<Long> cartIds = jdbcTemplate.query(getCartIdSql, (rs, rowNum) -> rs.getLong("cart_id"), userId);
        
        Long cartId;
        if (cartIds.isEmpty()) {
            // Create new cart
            String insertCartSql = "INSERT INTO cart (user_id, is_active, created_at, updated_at) VALUES (?, 1, NOW(), NOW())";
            jdbcTemplate.update(insertCartSql, userId);
            cartId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            // Use existing cart
            cartId = cartIds.get(0);
        }
        
        // Get product price
        String priceSql = "SELECT selling_price FROM products WHERE product_id = ?";
        BigDecimal price = jdbcTemplate.queryForObject(priceSql, BigDecimal.class, productId);
        
        // Check if item already exists in cart
        String checkSql = "SELECT cart_item_id, quantity FROM cart_items WHERE cart_id = ? AND product_id = ?";
        List<Map<String, Object>> existingItems = jdbcTemplate.query(checkSql, (rs, rowNum) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("cartItemId", rs.getLong("cart_item_id"));
            item.put("quantity", rs.getInt("quantity"));
            return item;
        }, cartId, productId);
        
        if (!existingItems.isEmpty()) {
            // Update existing item quantity
            Long cartItemId = (Long) existingItems.get(0).get("cartItemId");
            int newQuantity = (Integer) existingItems.get(0).get("quantity") + 1;
            
            String updateSql = """
                UPDATE cart_items 
                SET quantity = ?, subtotal = ? * ?, updated_at = NOW()
                WHERE cart_item_id = ?
                """;
            jdbcTemplate.update(updateSql, newQuantity, price, newQuantity, cartItemId);
            
            log.info("Updated existing cart item {} to quantity {}", cartItemId, newQuantity);
        } else {
            // Add new item to cart
            String insertSql = """
                INSERT INTO cart_items (cart_id, product_id, quantity, unit_price, subtotal, added_at, updated_at)
                VALUES (?, ?, 1, ?, ?, NOW(), NOW())
                """;
            jdbcTemplate.update(insertSql, cartId, productId, price, price);
            
            log.info("Added new item to cart: product {} for user {}", productId, userId);
        }
    }
}
