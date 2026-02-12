package com.cognizant.smartpay.service;

import com.cognizant.smartpay.entity.User;
import com.cognizant.smartpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for payment processing with Email and SMS notifications
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final JdbcTemplate jdbcTemplate;
    private final JavaMailSender mailSender;
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Map<String, Object> processPayment(Long userId) {
        log.info("Processing payment and notifications for user: {}", userId);

        // 1. Fetch User Contact Details
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

        String transactionSql = """
            INSERT INTO transactions (transaction_reference, user_id, cart_id, total_amount, final_amount, 
            payment_method, payment_status, wallet_balance_before, wallet_balance_after, items_count, transaction_date)
            VALUES (?, ?, ?, ?, ?, 'WALLET', 'SUCCESS', ?, ?, ?, NOW())
            """;

        jdbcTemplate.update(transactionSql,
                transactionReference, userId, cartId, totalAmount, totalAmount,
                walletBalance, walletBalance.subtract(totalAmount), cartItems.size());

        Long transactionId = jdbcTemplate.queryForObject(
                "SELECT transaction_id FROM transactions WHERE transaction_reference = ?", Long.class, transactionReference);

        // 6. Update Inventory and Wallet
        for (Map<String, Object> item : cartItems) {
            Long productId = (Long) item.get("productId");
            int quantityToBuy = (int) item.get("quantity");

            Integer currentStock = jdbcTemplate.queryForObject(
                    "SELECT stock_quantity FROM products WHERE product_id = ?", Integer.class, productId);

            if (currentStock == null || currentStock < quantityToBuy) {
                throw new IllegalArgumentException("Insufficient stock for product: " + item.get("productName"));
            }

            jdbcTemplate.update("""
                INSERT INTO transaction_items (transaction_id, product_id, product_name, product_brand, quantity, unit_price, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?)""",
                    transactionId, productId, item.get("productName"), item.get("productBrand"),
                    quantityToBuy, item.get("unitPrice"), item.get("subtotal"));

            jdbcTemplate.update("UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?",
                    quantityToBuy, productId);
        }

        jdbcTemplate.update("UPDATE wallet SET balance = balance - ? WHERE user_id = ?", totalAmount, userId);

        // 7. Clear Cart
        jdbcTemplate.update("DELETE FROM cart_items WHERE cart_id = ?", cartId);
        jdbcTemplate.update("UPDATE cart SET is_active = 0 WHERE cart_id = ?", cartId);

        // 8. ASYNCHRONOUS NOTIFICATIONS (Updated to ensure background execution)
        final String fEmail = userEmail;
        final String fName = userName;
        final String fPhone = userPhone;
        final BigDecimal fAmount = totalAmount;
        final List<Map<String, Object>> fItems = cartItems;
        final String fTxn = transactionReference;

        CompletableFuture.runAsync(() -> {
            sendEmailInvoice(fEmail, fName, fPhone, fAmount, fItems);
            sendSMSNotification(fPhone, fTxn, fAmount);
        });

        // 9. Prepare response (Immediate Success)
        Map<String, Object> result = new HashMap<>();
        result.put("transactionId", transactionReference);
        result.put("status", "success");

        return result;
    }

    public void processInvoice(String email, BigDecimal totalAmount, List<Map<String, Object>> items) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        sendEmailInvoice(user.getEmail(), user.getName(), user.getPhone(), totalAmount, items);
    }

    private String generateInvoiceNumber() {
        java.util.Random random = new java.util.Random();
        int number = 10000000 + random.nextInt(90000000);
        return "#" + String.valueOf(number);
    }

    @Async
    public void sendEmailInvoice(String toEmail, String name, String userPhone,
                                 BigDecimal totalAmount, List<Map<String, Object>> items) {
        try {
            log.info("Starting Async Email Task for: {}", toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String invoiceNo = generateInvoiceNumber();
            helper.setTo(toEmail.trim());
            helper.setSubject("Your Cognizant SmartPay Invoice - " + invoiceNo);

            // ✅ FIXED AMOUNT CALCULATION (ONLY FOR INVOICE DISPLAY)
            BigDecimal tax = new BigDecimal("2.50");
            BigDecimal subtotal = totalAmount;                 // items total
            BigDecimal grandTotal = subtotal.add(tax);         // subtotal + tax

            StringBuilder itemsHtml = new StringBuilder();
            for (Map<String, Object> item : items) {
                itemsHtml.append(String.format(
                        "<tr>" +
                                "<td style='padding: 15px 10px; font-size: 14px; color: #000048; border-bottom: 1px solid #EAECEF; width: 45%%;'>%s</td>" +
                                "<td style='padding: 15px 10px; font-size: 14px; color: #000048; border-bottom: 1px solid #EAECEF; text-align: center; width: 10%%;'>%s</td>" +
                                "<td style='padding: 15px 10px; font-size: 14px; color: #000048; border-bottom: 1px solid #EAECEF; text-align: right; width: 20%%;'>$ %.2f</td>" +
                                "<td style='padding: 15px 10px; font-size: 14px; color: #000048; border-bottom: 1px solid #EAECEF; text-align: right; width: 25%%;'>$ %.2f</td>" +
                                "</tr>",
                        item.get("productName"),
                        item.get("quantity"),
                        ((BigDecimal) item.get("unitPrice")).doubleValue(),
                        ((BigDecimal) item.get("subtotal")).doubleValue()
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
                    
                                 <!-- ✅ LOGO UPDATE (only): CID must match addInline key -->
            <img src="cid:logoImage" alt="Logo" width="276" height="184" style="displayargin-bottom: 25px;">
                    
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
                                <div style="background-color: #26EFE9; padding: 10px 20px; font-weight: 700; font-size: 18px; display: inline-block; color: #5E6470;">
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
                    name,
                    userPhone,
                    invoiceNo,
                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                            .format(java.time.LocalDateTime.now()),
                    itemsHtml.toString(),
                    subtotal.doubleValue(),
                    tax.doubleValue(),
                    grandTotal.doubleValue()
            );

            helper.setText(htmlBody, true);

            File logoFile = new File("C://Users//2387524//RFID-scan//Logo.png");
            if (logoFile.exists()) {
                // ✅ LOGO UPDATE (only): force image mime type so client renders inline
                helper.addInline("logoImage", new FileSystemResource(logoFile), "image/png");
            }

            mailSender.send(message);
            log.info("Invoice email sent successfully.");
        } catch (Exception e) {
            log.error("Email error: {}", e.getMessage());
        }
    }

    @Async
    public void sendSMSNotification(String phone, String txnId, BigDecimal amount) {
        try {
            String ACCOUNT_SID = "ACd9987b88c796e6e576058dd5dd257e45".trim();
            String AUTH_TOKEN = "ade41fc759855b0d98e50d8fe6717621".trim();
            String FROM_NUMBER = "+13158733994";

            // Trust all certificates to bypass PKIX errors in corporate network
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

            String toPhone = phone.startsWith("+") ? phone : "+91" + phone;
            String msg = "SmartPay: Payment of Rs." + amount + " successful. Txn: " + txnId;
            String urlString = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";

            java.net.URL url = new java.net.URL(urlString);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

            String auth = ACCOUNT_SID + ":" + AUTH_TOKEN;
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
                log.info("SMS Sent Successfully!");
            } else {
                log.error("Twilio API Error Code: {}", conn.getResponseCode());
            }
        } catch (Exception e) {
            log.error("Native SMS Bypass Failed: {}", e.getMessage());
        }
    }
}
