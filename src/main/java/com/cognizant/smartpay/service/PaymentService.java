package com.cognizant.smartpay.service;

<<<<<<< HEAD
import com.cognizant.smartpay.entity.User;
import com.cognizant.smartpay.repository.UserRepository;
=======
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
import com.twilio.Twilio;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
<<<<<<< HEAD
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
=======
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

<<<<<<< HEAD
import java.io.File;
import java.math.BigDecimal;
import java.security.SecureRandom;
=======
import java.math.BigDecimal;
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for payment processing with Email and SMS notifications
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final JdbcTemplate jdbcTemplate;
    private final JavaMailSender mailSender;
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Transactional
    public Map<String, Object> processPayment(Long userId) {
        log.info("Processing payment and notifications for user: {}", userId);

        // 1. Fetch User Contact Details (Using correct column names from your logs)
        String userSql = "SELECT email, phone, name FROM users WHERE user_id = ?";
        Map<String, Object> userData;
        try {
            userData = jdbcTemplate.queryForMap(userSql, userId);
        } catch (Exception e) {
            log.error("Error fetching user {}: {}", userId, e.getMessage());
            throw new IllegalArgumentException("User not found or database error");
        }

        String userEmail = (String) userData.get("email");
        String userPhone = (String) userData.get("phone");
        String userName = (String) userData.get("name");

        // 2. Get user's active cart items
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

        // 3. Calculate total
        BigDecimal totalAmount = cartItems.stream()
                .map(item -> (BigDecimal) item.get("subtotal"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Check wallet balance
        String walletSql = "SELECT balance FROM wallet WHERE user_id = ?";
        BigDecimal walletBalance = jdbcTemplate.queryForObject(walletSql, BigDecimal.class, userId);

        if (walletBalance == null || walletBalance.compareTo(totalAmount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        // 5. Generate Transaction
        Long cartId = (Long) cartItems.get(0).get("cartId");
        String transactionReference = "TXN" + System.currentTimeMillis();

        // Create transaction record
        String transactionSql = """
            INSERT INTO transactions (transaction_reference, user_id, cart_id, total_amount, final_amount, 
            payment_method, payment_status, wallet_balance_before, wallet_balance_after, items_count, transaction_date)
            VALUES (?, ?, ?, ?, ?, 'WALLET', 'SUCCESS', ?, ?, ?, NOW())
            """;

        jdbcTemplate.update(transactionSql,
                transactionReference, userId, cartId, totalAmount, totalAmount,
                walletBalance, walletBalance.subtract(totalAmount), cartItems.size());

        // Get generated ID
        Long transactionId = jdbcTemplate.queryForObject(
                "SELECT transaction_id FROM transactions WHERE transaction_reference = ?", Long.class, transactionReference);

        // 6. Update Inventory and Wallet
        for (Map<String, Object> item : cartItems) {
<<<<<<< HEAD
            Long productId = (Long) item.get("productId");
            int quantityToBuy = (int) item.get("quantity");

            // --- ADDED STOCK CHECK LOGIC ---
            Integer currentStock = jdbcTemplate.queryForObject(
                    "SELECT stock_quantity FROM products WHERE product_id = ?", Integer.class, productId);

            if (currentStock == null || currentStock < quantityToBuy) {
                log.error("Stock check failed for product {}. Available: {}, Requested: {}", productId, currentStock, quantityToBuy);
                throw new IllegalArgumentException("Insufficient stock for product: " + item.get("productName"));
            }
            // -------------------------------

=======
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
            // Insert into transaction_items
            jdbcTemplate.update("""
                INSERT INTO transaction_items (transaction_id, product_id, product_name, product_brand, quantity, unit_price, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?)""",
<<<<<<< HEAD
                    transactionId, productId, item.get("productName"), item.get("productBrand"),
                    quantityToBuy, item.get("unitPrice"), item.get("subtotal"));

            // Decrease product stock
            jdbcTemplate.update("UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?",
                    quantityToBuy, productId);
=======
                    transactionId, item.get("productId"), item.get("productName"), item.get("productBrand"),
                    item.get("quantity"), item.get("unitPrice"), item.get("subtotal"));

            // Decrease product stock
            jdbcTemplate.update("UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?",
                    item.get("quantity"), item.get("productId"));
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
        }

        // Update Wallet
        jdbcTemplate.update("UPDATE wallet SET balance = balance - ? WHERE user_id = ?", totalAmount, userId);

        // 7. Clear Cart
        jdbcTemplate.update("DELETE FROM cart_items WHERE cart_id = ?", cartId);
        jdbcTemplate.update("UPDATE cart SET is_active = 0 WHERE cart_id = ?", cartId);

        // 8. SEND NOTIFICATIONS
<<<<<<< HEAD
        sendEmailInvoice(userEmail, userName, userPhone, totalAmount, cartItems);
        sendSMSNotification(userPhone, transactionReference, totalAmount);
=======
        sendEmailInvoice(userEmail, userName, transactionReference, totalAmount);
        // sendSMSNotification(userPhone, transactionReference, totalAmount);
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e

        // Prepare response
        Map<String, Object> result = new HashMap<>();
        result.put("transactionId", transactionReference);
        result.put("status", "success");
<<<<<<< HEAD
=======
        result.put("newBalance", walletBalance.subtract(totalAmount));
        result.put("amount", totalAmount);
        result.put("timestamp", LocalDateTime.now().toString());
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e

        return result;
    }

<<<<<<< HEAD
    // Inside your Service class
    @Autowired
    private UserRepository userRepository;

    public void processInvoice(String email, BigDecimal totalAmount, List<Map<String, Object>> items) {
        // Explicitly fetch the User entity from the UserRepository
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Ensure user.getPhone() is the value passed to the 'userPhone' parameter
        sendEmailInvoice(user.getEmail(), user.getName(), user.getPhone(), totalAmount, items);
    }

    private String generateInvoiceNumber() {
        // Generates a unique 8-digit number starting with #
        java.util.Random random = new java.util.Random();
        int number = 10000000 + random.nextInt(90000000);
        return "#" + String.valueOf(number);
    }

    private void sendEmailInvoice(String toEmail, String name, String userPhone, BigDecimal totalAmount, List<Map<String, Object>> items) {
=======
    private void sendEmailInvoice(String toEmail, String name, String txnId, BigDecimal amount) {
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

<<<<<<< HEAD
            String invoiceNo = generateInvoiceNumber();
            helper.setTo(toEmail);
            helper.setSubject("Your Cognizant SmartPay Invoice - " + invoiceNo);

            BigDecimal tax = new BigDecimal("2.50");
            BigDecimal subtotal = totalAmount.subtract(tax);

            StringBuilder itemsHtml = new StringBuilder();
            for (Map<String, Object> item : items) {
                itemsHtml.append(String.format(
                        "<tr>" +
                                "<td style='padding: 15px 10px; font-size: 14px; color: #000048; border-bottom: 1px solid #EAECEF; width: 45%%;'>%s</td>" +
                                "<td style='padding: 15px 10px; font-size: 14px; color: #000048; border-bottom: 1px solid #EAECEF; text-align: center; width: 10%%;'>%s</td>" +
                                "<td style='padding: 15px 10px; font-size: 14px; color: #000048; border-bottom: 1px solid #EAECEF; text-align: right; width: 20%%;'>$ %.2f</td>" +
                                "<td style='padding: 15px 10px; font-size: 14px; color: #000048; border-bottom: 1px solid #EAECEF; text-align: right; width: 25%%;'>$ %.2f</td>" +
                                "</tr>",
                        item.get("productName"), item.get("quantity"), item.get("unitPrice"), item.get("subtotal")
                ));
            }

            String htmlBody = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { margin: 0; padding: 0; background-color: #FFFFFF; font-family: sans-serif; }
                .container { width: 100%%; max-width: 650px; margin: 0 auto; padding: 20px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div style="text-align: center; margin-bottom: 30px;">
                    <img src="cid:logoImage" alt="Logo" width="276" height="184" Top="-11" Left="143">
                  </div>

                <div style="margin-bottom: 25px;">
                    <p style="margin: 0; font-size: 11px; color: #8A92A6; font-weight: 600;">BILL TO</p>
                    <p style="margin: 4px 0 0 0; font-size: 14px; color: #000048;">Name : %s</p>
                    <p style="margin: 2px 0 0 0; font-size: 14px; color: #000048;">Mobile no: %s</p>
                </div>

                <table style="width: 100%%; border-collapse: collapse; margin-bottom: 30px;">
                    <tr>
                        <td style="width: 33%%; vertical-align: top;">
                            <p style="margin: 0; font-size: 11px; color: #8A92A6; font-weight: 600;">INVOICE NUMBER</p>
                            <p style="margin: 2px 0 0 0; font-size: 14px; font-weight: 700; color: #000048;">%s</p>
                        </td>
                        <td style="width: 33%%; vertical-align: top;">
                            <p style="margin: 0; font-size: 11px; color: #8A92A6; font-weight: 600;">DATE & TIME</p>
                            <p style="margin: 2px 0 0 0; font-size: 14px; font-weight: 700; color: #000048;">%s</p>
                        </td>
                        <td style="width: 33%%; vertical-align: top;">
                            <p style="margin: 0; font-size: 11px; color: #8A92A6; font-weight: 600;">PAYMENT METHOD</p>
                            <p style="margin: 2px 0 0 0; font-size: 14px; font-weight: 700; color: #000048;">Wallet payment</p>
                        </td>
                    </tr>
                </table>

                <table style="width: 100%%; border-collapse: collapse; background-color: #000048;">
                    <tr>
                        <th style="padding: 12px 10px; color: #FFFFFF; font-size: 12px; text-align: left; width: 45%%;">PRODUCT DETAILS</th>
                        <th style="padding: 12px 10px; color: #FFFFFF; font-size: 12px; text-align: center; width: 10%%;">QTY</th>
                        <th style="padding: 12px 10px; color: #FFFFFF; font-size: 12px; text-align: right; width: 20%%;">PRICE</th>
                        <th style="padding: 12px 10px; color: #FFFFFF; font-size: 12px; text-align: right; width: 25%%;">SUBTOTAL</th>
                    </tr>
                </table>
                <table style="width: 100%%; border-collapse: collapse;">
                    %s
                </table>

                <table style="width: 100%%; margin-top: 20px;">
                    <tr>
                        <td style="font-size: 14px; color: #8A92A6; font-weight: 600; padding: 5px 0;">Subtotal</td>
                        <td style="font-size: 14px; color: #000048; font-weight: 700; text-align: right;">$ %.2f</td>
                    </tr>
                    <tr>
                        <td style="font-size: 14px; color: #000048; padding: 5px 0;">Tax</td>
                        <td style="font-size: 14px; color: #000048; text-align: right;">$ %.2f</td>
                    </tr>
                </table>

                <div style="border-top: 1px solid #EAECEF; margin: 15px 0;"></div>

                <table style="width: 100%%; margin-bottom: 40px;">
                    <tr>
                        <td style="font-size: 18px; font-weight: 700; color: #000048;">Order Total</td>
                        <td style="text-align: right;">
                            <div style="background-color: #26EFE9; color: background: #5E6470;padding: 10px 20px; font-weight: 700; font-size: 18px; display: inline-block;">
                                $ %.2f
                            </div>
                        </td>
                    </tr>
                </table>
                <div style="text-align: center; padding-bottom: 30px;">
                    <p style="font-size: 16px; font-weight: 600; color: #000048;">Thank you for shopping with us</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(
                    name, userPhone, invoiceNo,
                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(java.time.LocalDateTime.now()),
                    itemsHtml.toString(), subtotal, tax, totalAmount
            );
            helper.setText(htmlBody, true);

            File logoFile = new File("C:/Users/2426479/OneDrive - Cognizant/Desktop/Logo.png");

            if (logoFile.exists()) {
                FileSystemResource res = new FileSystemResource(logoFile);
                helper.addInline("logoImage", res);
            }

            mailSender.send(message);
            log.info("Invoice email sent successfully.");
        } catch (Exception e) {
            log.error("Email error: {}", e.getMessage());
        }
    }
    private void sendSMSNotification(String phone, String txnId, BigDecimal amount) {
        try {
            String ACCOUNT_SID = "ACd9987b88c796e6e576058dd5dd257e45";
            String AUTH_TOKEN = "ade41fc759855b0d98e50d8fe6717621";
            String FROM_NUMBER = "+13158733994";

            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    }
            };
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            String toPhone = phone.startsWith("+") ? phone : "+91" + phone;
            String msg = "SmartPay: Payment of Rs." + amount + " successful. Txn: " + txnId;
            String urlString = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";

            java.net.URL url = new java.net.URL(urlString);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

            String auth = ACCOUNT_SID.trim() + ":" + AUTH_TOKEN.trim();
            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String postData = "To=" + java.net.URLEncoder.encode(toPhone, "UTF-8") +
                    "&From=" + java.net.URLEncoder.encode(FROM_NUMBER, "UTF-8") +
                    "&Body=" + java.net.URLEncoder.encode(msg, "UTF-8");

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes());
            }

            if (conn.getResponseCode() == 201 || conn.getResponseCode() == 200) {
                log.info("SMS Sent Successfully via Native Java! Response: {}", conn.getResponseCode());
            } else {
                log.error("Twilio API Error Code: {}", conn.getResponseCode());
            }

        } catch (Exception e) {
            log.error("Native SMS Bypass Failed: {}", e.getMessage());
        }
    }
=======
            helper.setTo(toEmail);
            helper.setSubject("Payment Receipt - Cognizant SmartPay");

            String htmlBody = String.format(
                    "<div style='font-family: Arial, sans-serif; border: 1px solid #eee; padding: 20px;'>" +
                            "<h2 style='color: #2e7d32;'>Payment Successful!</h2>" +
                            "<p>Hi %s,</p>" +
                            "<p>Thank you for your purchase. Here is your transaction summary:</p>" +
                            "<table style='width: 100%%; border-collapse: collapse;'>" +
                            "<tr><td style='padding: 8px; border-bottom: 1px solid #eee;'><strong>Transaction ID:</strong></td><td>%s</td></tr>" +
                            "<tr><td style='padding: 8px; border-bottom: 1px solid #eee;'><strong>Amount Paid:</strong></td><td style='color: #2e7d32; font-weight: bold;'>₹%.2f</td></tr>" +
                            "</table>" +
                            "<p>The amount has been debited from your SmartPay Wallet.</p>" +
                            "</div>", name, txnId, amount);

            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Receipt email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
        }
    }

//    private void sendSMSNotification(String phone, String txnId, BigDecimal amount) {
//        try {
//            // 1. YOUR CREDENTIALS
//            String ACCOUNT_SID = "ACd9987b88c796e6e576058dd5dd257e45";
//            String AUTH_TOKEN = "ade41fc759855b0d98e50d8fe6717621";
//            String FROM_NUMBER = "+13158733994"; // Your Twilio Number
//
//
//
//            // 2. BYPASS SSL GLOBALLY FOR THIS REQUEST
//            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
//                    new javax.net.ssl.X509TrustManager() {
//                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
//                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
//                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
//                    }
//            };
//            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
//            sc.init(null, trustAllCerts, new java.security.SecureRandom());
//            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
//
//            // 3. PREPARE DATA
//            String toPhone = phone.startsWith("+") ? phone : "+91" + phone;
//            String msg = "SmartPay: Payment of Rs." + amount + " successful. Txn: " + txnId;
//            String urlString = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";
//
//            // 4. SEND VIA HTTP POST
//            java.net.URL url = new java.net.URL(urlString);
//            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
//
//            String auth = ACCOUNT_SID.trim() + ":" + AUTH_TOKEN.trim(); // Added .trim() to remove hidden spaces
//            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
//            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
//            conn.setRequestMethod("POST");
//            conn.setDoOutput(true);
//
//            String postData = "To=" + java.net.URLEncoder.encode(toPhone, "UTF-8") +
//                    "&From=" + java.net.URLEncoder.encode(FROM_NUMBER, "UTF-8") +
//                    "&Body=" + java.net.URLEncoder.encode(msg, "UTF-8");
//
//            try (java.io.OutputStream os = conn.getOutputStream()) {
//                os.write(postData.getBytes());
//            }
//
//            if (conn.getResponseCode() == 201 || conn.getResponseCode() == 200) {
//                log.info("SMS Sent Successfully via Native Java! Response: {}", conn.getResponseCode());
//            } else {
//                log.error("Twilio API Error Code: {}", conn.getResponseCode());
//            }
//
//        } catch (Exception e) {
//            log.error("Native SMS Bypass Failed: {}", e.getMessage());
//        }
//    }
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
}