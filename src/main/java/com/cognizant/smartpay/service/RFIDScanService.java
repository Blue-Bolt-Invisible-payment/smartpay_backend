package com.cognizant.smartpay.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RFIDScanService {

    private final JdbcTemplate jdbcTemplate;

    public RFIDScanService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void processScannedTags(List<String> scannedTags){
        System.out.println("Listof scanned tag:"+scannedTags);
        //log.info("Adding item by RFID tag..");
        // Logic to process scanned tags and add products to cart
        // Get currently logged-in user
        String getUserIdSql = "SELECT user_id FROM users WHERE login_status = ? ";
        List<Long> userIds = jdbcTemplate.query(getUserIdSql, (rs, rowNum) -> rs.getLong("user_id"), 'Y');
        Long userId;
        Long cartId;
        if(userIds != null && userIds.size() > 0){
            userId = userIds.get(0);
            // Get or create cart for user - ensure only one active cart per user
            String getCartIdSql = "SELECT cart_id FROM cart WHERE user_id = ? AND is_active = 1";
            List<Long> cartIds = jdbcTemplate.query(getCartIdSql, (rs, rowNum) -> rs.getLong("cart_id"), userId);

            if (cartIds.isEmpty()) {
                // Create new cart
                String insertCartSql = "INSERT INTO cart (user_id, is_active, created_at, updated_at) VALUES (?, 1, NOW(), NOW())";
                jdbcTemplate.update(insertCartSql, userId);
                cartId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            } else {
                // Use existing cart
                cartId = cartIds.get(0);
            }
            // Process each scanned tag
            scannedTagsOneByOne(userId,cartId,scannedTags);

        } else{
            throw new IllegalArgumentException("No logged-in user found");
        }
    }

    public void scannedTagsOneByOne(Long userId,Long cartId,List<String> scannedTags){

        for(String tag : scannedTags){
            // First, get product ID from RFID tag
            String productSql = "SELECT product_id FROM rfid_tags WHERE rfid_tag = ? AND is_active = 1";
            List<Long> productIds = jdbcTemplate.query(productSql, (rs, rowNum) -> rs.getLong("product_id"), tag);

            if (productIds == null || productIds.size() == 0) {
                throw new IllegalArgumentException("Product not found for RFID tag: " + tag);
            }
            Long productId = productIds.get(0);

            // Get product price
            String priceSql = "SELECT selling_price FROM products WHERE product_id = ?";
            Double price = jdbcTemplate.queryForObject(priceSql, Double.class, productId);


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

                //log.info("Updated existing cart item {} to quantity {}", cartItemId, newQuantity);
            } else {
                // Add new item to cart
                String insertSql = """
                INSERT INTO cart_items (cart_id, product_id, quantity, unit_price, subtotal, added_at, updated_at)
                VALUES (?, ?, 1, ?, ?, NOW(), NOW())
                """;
                jdbcTemplate.update(insertSql, cartId, productId, price, price);

                //log.info("Added new item to cart: product {} for user {}", productId, userId);
            }
        }
    }
}
