CREATE DATABASE  IF NOT EXISTS `smartpay_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `smartpay_db`;
-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: smartpay_db
-- ------------------------------------------------------
-- Server version	8.4.4

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `biometrics`
--

DROP TABLE IF EXISTS `biometrics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biometrics` (
  `biometric_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `fingerprint_hash` varchar(512) NOT NULL COMMENT 'SHA-512 hash of fingerprint template',
  `fingerprint_template` blob NOT NULL COMMENT 'Encrypted biometric template',
  `credential_id` varchar(255) DEFAULT NULL COMMENT 'WebAuthn credential ID',
  `public_key` blob,
  `device_type` varchar(100) DEFAULT NULL COMMENT 'Device used for enrollment (e.g., Android, Touch ID, USB Scanner)',
  `enrollment_method` varchar(50) DEFAULT NULL,
  `enrolled_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `last_verified_at` timestamp NULL DEFAULT NULL,
  `verification_count` int DEFAULT '0',
  `is_active` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`biometric_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_credential_id` (`credential_id`),
  KEY `idx_is_active` (`is_active`),
  CONSTRAINT `biometrics_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Biometric authentication data';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biometrics`
--

LOCK TABLES `biometrics` WRITE;
/*!40000 ALTER TABLE `biometrics` DISABLE KEYS */;
INSERT INTO `biometrics` VALUES (12,17,'de3e80ec5d65dd59c341c5b103d1a630034832a805ccd624fa08e30b45e84877','','uCc5LkhUwXvVQIFopDk7jH0uYl4PMUPdWM5OJTxcVNM=',_binary '0Y0*†H\Î=*†H\Î=B\0¼Š·Ylyw8tÕˆC‹tuu­nT¶ì¨©KM\Ã,3\ì-]r`t‚ŠTñ¸b.­‚\äÞ°®#\ÕðiC]½®8\Ù>','Windows Hello','webauthn','2025-12-08 09:01:37','2025-12-08 11:28:34',5,1),(13,18,'26d48312abf0804f152ccef0fa45aa06e4ec124cc9f0e6290b69792b01c69a31','','xQYIfJlS7hi+OUbhaiosIm4S726B8AU0dCaes8iP/uw=',_binary '0Y0*†H\Î=*†H\Î=B\0b\Ø‡‚¿\ÉDIõ\ï\æ 59«Pœñt¦3\Îa€”´&þ$¡X\ÍN9X€š\Æ\â\Ð{\ÊÑªk\ÉF@\Ö4rmEŽT','Windows Hello','webauthn','2025-12-08 09:06:56',NULL,0,1);
/*!40000 ALTER TABLE `biometrics` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cart`
--

DROP TABLE IF EXISTS `cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart` (
  `cart_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_active` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`cart_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_active` (`is_active`),
  CONSTRAINT `cart_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User shopping carts';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart`
--

LOCK TABLES `cart` WRITE;
/*!40000 ALTER TABLE `cart` DISABLE KEYS */;
/*!40000 ALTER TABLE `cart` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cart_items`
--

DROP TABLE IF EXISTS `cart_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_items` (
  `cart_item_id` bigint NOT NULL AUTO_INCREMENT,
  `cart_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `unit_price` decimal(10,2) NOT NULL,
  `subtotal` decimal(10,2) NOT NULL,
  `added_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`cart_item_id`),
  KEY `idx_cart_id` (`cart_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_cart_items_cart_product` (`cart_id`,`product_id`),
  CONSTRAINT `cart_items_ibfk_1` FOREIGN KEY (`cart_id`) REFERENCES `cart` (`cart_id`) ON DELETE CASCADE,
  CONSTRAINT `cart_items_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`),
  CONSTRAINT `cart_items_chk_1` CHECK ((`quantity` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Cart item details';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_items`
--

LOCK TABLES `cart_items` WRITE;
/*!40000 ALTER TABLE `cart_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `cart_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `category_id` int NOT NULL AUTO_INCREMENT,
  `category_name` varchar(50) NOT NULL,
  `description` text,
  `icon_url` varchar(255) DEFAULT NULL,
  `display_order` int DEFAULT '0',
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `category_name` (`category_name`),
  KEY `idx_display_order` (`display_order`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Product categories';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,'Fruits','Fresh fruits and seasonal produce',NULL,1,1,'2025-12-08 09:33:23'),(2,'Vegetables','Fresh vegetables and greens',NULL,2,1,'2025-12-08 09:33:23'),(3,'Dairy','Milk, cheese, yogurt and dairy products',NULL,3,1,'2025-12-08 09:33:23'),(4,'Bakery','Bread, cakes, pastries and baked goods',NULL,4,1,'2025-12-08 09:33:23'),(5,'Beverages','Soft drinks, juices, tea, coffee',NULL,5,1,'2025-12-08 09:33:23'),(6,'Snacks','Chips, biscuits, namkeen and snacks',NULL,6,1,'2025-12-08 09:33:23'),(7,'Grocery','Rice, pulses, oil and staples',NULL,7,1,'2025-12-08 09:33:23'),(8,'Personal Care','Soaps, shampoos, cosmetics',NULL,8,1,'2025-12-08 09:33:23'),(9,'Household','Cleaning supplies and household items',NULL,9,1,'2025-12-08 09:33:23'),(10,'Frozen Foods','Frozen vegetables, ready-to-eat items',NULL,10,1,'2025-12-08 09:33:23');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `product_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `brand` varchar(50) DEFAULT NULL,
  `category_id` int DEFAULT NULL,
  `description` text,
  `mrp` decimal(10,2) NOT NULL COMMENT 'Maximum Retail Price',
  `selling_price` decimal(10,2) NOT NULL COMMENT 'Actual selling price',
  `discount_percentage` decimal(5,2) DEFAULT '0.00',
  `unit` varchar(20) DEFAULT 'piece',
  `weight` varchar(20) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `barcode` varchar(50) DEFAULT NULL,
  `stock_quantity` int DEFAULT '0',
  `min_stock_level` int DEFAULT '10' COMMENT 'Alert threshold for low stock',
  `is_available` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `barcode` (`barcode`),
  KEY `idx_category` (`category_id`),
  KEY `idx_availability` (`is_available`),
  KEY `idx_name` (`name`),
  KEY `idx_barcode` (`barcode`),
  KEY `idx_products_category_available` (`category_id`,`is_available`),
  CONSTRAINT `products_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`),
  CONSTRAINT `products_chk_1` CHECK ((`selling_price` <= `mrp`)),
  CONSTRAINT `products_chk_2` CHECK ((`stock_quantity` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Product catalog';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (1,'Fresh Apples','Farm Fresh',1,'Red delicious apples',150.00,120.00,20.00,'1 kg',NULL,NULL,'BAR001',100,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(2,'Bananas','Organic',1,'Ripe yellow bananas',60.00,50.00,16.67,'1 dozen',NULL,NULL,'BAR002',150,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(3,'Oranges','Citrus Fresh',1,'Juicy oranges',100.00,85.00,15.00,'1 kg',NULL,NULL,'BAR003',80,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(4,'Mangoes (Alphonso)','Premium',1,'King of fruits',500.00,450.00,10.00,'1 dozen',NULL,NULL,'BAR004',50,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(5,'Grapes','Green Valley',1,'Seedless green grapes',120.00,100.00,16.67,'500g',NULL,NULL,'BAR005',60,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(6,'Tomatoes','Fresh Farm',2,'Fresh red tomatoes',40.00,35.00,12.50,'1 kg',NULL,NULL,'BAR006',200,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(7,'Onions','Local',2,'Red onions',35.00,30.00,14.29,'1 kg',NULL,NULL,'BAR007',250,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(8,'Potatoes','Farm Fresh',2,'Fresh potatoes',30.00,25.00,16.67,'1 kg',NULL,NULL,'BAR008',300,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(9,'Carrots','Organic',2,'Fresh carrots',50.00,42.00,16.00,'500g',NULL,NULL,'BAR009',120,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(10,'Spinach','Green Leaf',2,'Fresh spinach leaves',25.00,20.00,20.00,'250g',NULL,NULL,'BAR010',100,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(11,'Full Cream Milk','Amul',3,'Fresh full cream milk',60.00,58.00,3.33,'1 liter',NULL,NULL,'BAR011',150,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(12,'Paneer','Mother Dairy',3,'Fresh cottage cheese',90.00,85.00,5.56,'200g',NULL,NULL,'BAR012',80,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(13,'Curd','Nestle',3,'Fresh curd/yogurt',50.00,45.00,10.00,'400g',NULL,NULL,'BAR013',100,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(14,'Butter','Amul',3,'Fresh butter',55.00,50.00,9.09,'100g',NULL,NULL,'BAR014',120,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(15,'Cheese Slices','Britannia',3,'Processed cheese slices',150.00,135.00,10.00,'200g',NULL,NULL,'BAR015',90,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(16,'White Bread','Britannia',4,'Fresh white bread',45.00,40.00,11.11,'400g',NULL,NULL,'BAR016',100,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(17,'Brown Bread','Modern',4,'Whole wheat bread',50.00,45.00,10.00,'400g',NULL,NULL,'BAR017',80,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(18,'Butter Cookies','Parle',4,'Butter cookies',30.00,25.00,16.67,'200g',NULL,NULL,'BAR018',150,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(19,'Cake Rusk','Britannia',4,'Crispy cake rusk',35.00,30.00,14.29,'200g',NULL,NULL,'BAR019',120,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(20,'Chocolate Cake','Monginis',4,'Fresh chocolate cake',250.00,225.00,10.00,'500g',NULL,NULL,'BAR020',40,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(21,'Coca Cola','Coca Cola',5,'Carbonated soft drink',40.00,38.00,5.00,'750ml',NULL,NULL,'BAR021',200,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(22,'Pepsi','Pepsi',5,'Carbonated soft drink',40.00,38.00,5.00,'750ml',NULL,NULL,'BAR022',200,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(23,'Orange Juice','Real',5,'Fresh orange juice',80.00,72.00,10.00,'1 liter',NULL,NULL,'BAR023',100,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(24,'Green Tea','Lipton',5,'Green tea bags',150.00,135.00,10.00,'25 bags',NULL,NULL,'BAR024',80,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(25,'Coffee Powder','Nescafe',5,'Instant coffee',200.00,180.00,10.00,'50g',NULL,NULL,'BAR025',90,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(26,'Lays Classic','Lays',6,'Potato chips',20.00,18.00,10.00,'50g',NULL,NULL,'BAR026',300,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(27,'Kurkure','Kurkure',6,'Masala munch snack',20.00,18.00,10.00,'50g',NULL,NULL,'BAR027',250,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(28,'Parle-G Biscuits','Parle',6,'Glucose biscuits',10.00,9.00,10.00,'100g',NULL,NULL,'BAR028',400,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(29,'Britannia Good Day','Britannia',6,'Butter cookies',30.00,27.00,10.00,'150g',NULL,NULL,'BAR029',200,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07'),(30,'Haldiram Namkeen','Haldiram',6,'Bhujia mix',40.00,36.00,10.00,'200g',NULL,NULL,'BAR030',180,10,1,'2025-12-08 09:34:07','2025-12-08 09:34:07');
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rfid_tags`
--

DROP TABLE IF EXISTS `rfid_tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rfid_tags` (
  `rfid_id` bigint NOT NULL AUTO_INCREMENT,
  `rfid_tag` varchar(50) NOT NULL,
  `product_id` bigint NOT NULL,
  `assigned_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_active` tinyint(1) DEFAULT '1',
  `last_scanned_at` timestamp NULL DEFAULT NULL,
  `scan_count` int DEFAULT '0',
  PRIMARY KEY (`rfid_id`),
  UNIQUE KEY `rfid_tag` (`rfid_tag`),
  KEY `idx_rfid_tag` (`rfid_tag`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_is_active` (`is_active`),
  CONSTRAINT `rfid_tags_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RFID tag assignments';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rfid_tags`
--

LOCK TABLES `rfid_tags` WRITE;
/*!40000 ALTER TABLE `rfid_tags` DISABLE KEYS */;
INSERT INTO `rfid_tags` VALUES (1,'RFID0001',1,'2025-12-08 09:34:07',1,NULL,0),(2,'RFID0002',2,'2025-12-08 09:34:07',1,NULL,0),(3,'RFID0003',3,'2025-12-08 09:34:07',1,NULL,0),(4,'RFID0004',4,'2025-12-08 09:34:07',1,NULL,0),(5,'RFID0005',5,'2025-12-08 09:34:07',1,NULL,0),(6,'RFID0006',6,'2025-12-08 09:34:07',1,NULL,0),(7,'RFID0007',7,'2025-12-08 09:34:07',1,NULL,0),(8,'RFID0008',8,'2025-12-08 09:34:07',1,NULL,0),(9,'RFID0009',9,'2025-12-08 09:34:07',1,NULL,0),(10,'RFID0010',10,'2025-12-08 09:34:07',1,NULL,0),(11,'RFID0011',11,'2025-12-08 09:34:07',1,NULL,0),(12,'RFID0012',12,'2025-12-08 09:34:07',1,NULL,0),(13,'RFID0013',13,'2025-12-08 09:34:07',1,NULL,0),(14,'RFID0014',14,'2025-12-08 09:34:07',1,NULL,0),(15,'RFID0015',15,'2025-12-08 09:34:07',1,NULL,0),(16,'RFID0016',16,'2025-12-08 09:34:07',1,NULL,0),(17,'RFID0017',17,'2025-12-08 09:34:07',1,NULL,0),(18,'RFID0018',18,'2025-12-08 09:34:07',1,NULL,0),(19,'RFID0019',19,'2025-12-08 09:34:07',1,NULL,0),(20,'RFID0020',20,'2025-12-08 09:34:07',1,NULL,0),(21,'RFID0021',21,'2025-12-08 09:34:07',1,NULL,0),(22,'RFID0022',22,'2025-12-08 09:34:07',1,NULL,0),(23,'RFID0023',23,'2025-12-08 09:34:07',1,NULL,0),(24,'RFID0024',24,'2025-12-08 09:34:07',1,NULL,0),(25,'RFID0025',25,'2025-12-08 09:34:07',1,NULL,0),(26,'RFID0026',26,'2025-12-08 09:34:07',1,NULL,0),(27,'RFID0027',27,'2025-12-08 09:34:07',1,NULL,0),(28,'RFID0028',28,'2025-12-08 09:34:07',1,NULL,0),(29,'RFID0029',29,'2025-12-08 09:34:07',1,NULL,0),(30,'RFID0030',30,'2025-12-08 09:34:07',1,NULL,0);
/*!40000 ALTER TABLE `rfid_tags` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_logs`
--

DROP TABLE IF EXISTS `system_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_logs` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `log_level` enum('INFO','WARNING','ERROR','CRITICAL') DEFAULT 'INFO',
  `log_type` enum('AUTH','RFID_SCAN','PAYMENT','CART','SYSTEM') NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `message` text NOT NULL,
  `details` json DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `user_id` (`user_id`),
  KEY `idx_log_level` (`log_level`),
  KEY `idx_log_type` (`log_type`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `system_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='System audit logs';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_logs`
--

LOCK TABLES `system_logs` WRITE;
/*!40000 ALTER TABLE `system_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `system_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transaction_items`
--

DROP TABLE IF EXISTS `transaction_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transaction_items` (
  `transaction_item_id` bigint NOT NULL AUTO_INCREMENT,
  `transaction_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `product_name` varchar(100) NOT NULL,
  `product_brand` varchar(50) DEFAULT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `discount_per_item` decimal(10,2) DEFAULT '0.00',
  `subtotal` decimal(10,2) NOT NULL,
  PRIMARY KEY (`transaction_item_id`),
  KEY `product_id` (`product_id`),
  KEY `idx_transaction_id` (`transaction_id`),
  CONSTRAINT `transaction_items_ibfk_1` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`transaction_id`) ON DELETE CASCADE,
  CONSTRAINT `transaction_items_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Transaction line items';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transaction_items`
--

LOCK TABLES `transaction_items` WRITE;
/*!40000 ALTER TABLE `transaction_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `transaction_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transactions`
--

DROP TABLE IF EXISTS `transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transactions` (
  `transaction_id` bigint NOT NULL AUTO_INCREMENT,
  `transaction_reference` varchar(50) NOT NULL,
  `user_id` bigint NOT NULL,
  `cart_id` bigint DEFAULT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `discount_amount` decimal(10,2) DEFAULT '0.00',
  `tax_amount` decimal(10,2) DEFAULT '0.00',
  `final_amount` decimal(10,2) NOT NULL,
  `payment_method` enum('WALLET','CARD','UPI','CASH') DEFAULT 'WALLET',
  `payment_status` enum('PENDING','SUCCESS','FAILED','REFUNDED') DEFAULT 'PENDING',
  `wallet_balance_before` decimal(10,2) DEFAULT NULL,
  `wallet_balance_after` decimal(10,2) DEFAULT NULL,
  `transaction_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `items_count` int DEFAULT NULL,
  `remarks` text,
  PRIMARY KEY (`transaction_id`),
  UNIQUE KEY `transaction_reference` (`transaction_reference`),
  KEY `cart_id` (`cart_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_transaction_ref` (`transaction_reference`),
  KEY `idx_status` (`payment_status`),
  KEY `idx_date` (`transaction_date`),
  KEY `idx_transactions_user_date` (`user_id`,`transaction_date`),
  CONSTRAINT `transactions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `transactions_ibfk_2` FOREIGN KEY (`cart_id`) REFERENCES `cart` (`cart_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Payment transactions';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transactions`
--

LOCK TABLES `transactions` WRITE;
/*!40000 ALTER TABLE `transactions` DISABLE KEYS */;
/*!40000 ALTER TABLE `transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `phone` varchar(15) DEFAULT NULL,
  `biometric_enabled` tinyint(1) DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status` enum('ACTIVE','INACTIVE','SUSPENDED') DEFAULT 'ACTIVE',
  `last_login` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User account information';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (17,'Farheen Sulthana','farheensulthana60@gmail.com','7406809511',1,1,'2025-12-08 09:01:37','2025-12-08 09:01:37','ACTIVE',NULL),(18,'Ruksar Sulthana','farheensulthana601@gmail.com','7406809512',1,1,'2025-12-08 09:06:56','2025-12-08 09:06:56','ACTIVE',NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `vw_cart_summary`
--

DROP TABLE IF EXISTS `vw_cart_summary`;
/*!50001 DROP VIEW IF EXISTS `vw_cart_summary`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `vw_cart_summary` AS SELECT 
 1 AS `cart_id`,
 1 AS `user_id`,
 1 AS `user_name`,
 1 AS `email`,
 1 AS `items_count`,
 1 AS `cart_total`,
 1 AS `last_updated`,
 1 AS `is_active`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `vw_products_full`
--

DROP TABLE IF EXISTS `vw_products_full`;
/*!50001 DROP VIEW IF EXISTS `vw_products_full`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `vw_products_full` AS SELECT 
 1 AS `product_id`,
 1 AS `product_name`,
 1 AS `brand`,
 1 AS `category_name`,
 1 AS `description`,
 1 AS `mrp`,
 1 AS `selling_price`,
 1 AS `discount_percentage`,
 1 AS `unit`,
 1 AS `stock_quantity`,
 1 AS `is_available`,
 1 AS `rfid_tag`,
 1 AS `barcode`,
 1 AS `image_url`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `vw_users_wallet`
--

DROP TABLE IF EXISTS `vw_users_wallet`;
/*!50001 DROP VIEW IF EXISTS `vw_users_wallet`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `vw_users_wallet` AS SELECT 
 1 AS `user_id`,
 1 AS `name`,
 1 AS `email`,
 1 AS `phone`,
 1 AS `biometric_enabled`,
 1 AS `status`,
 1 AS `created_at`,
 1 AS `last_login`,
 1 AS `wallet_balance`,
 1 AS `currency`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `wallet`
--

DROP TABLE IF EXISTS `wallet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallet` (
  `wallet_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `balance` decimal(10,2) DEFAULT '0.00',
  `currency` varchar(3) DEFAULT 'INR',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`wallet_id`),
  UNIQUE KEY `user_id` (`user_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `wallet_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `wallet_chk_1` CHECK ((`balance` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='User digital wallets';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wallet`
--

LOCK TABLES `wallet` WRITE;
/*!40000 ALTER TABLE `wallet` DISABLE KEYS */;
INSERT INTO `wallet` VALUES (17,17,1000.00,'INR','2025-12-08 09:01:37','2025-12-08 09:01:37'),(18,18,1000.00,'INR','2025-12-08 09:06:56','2025-12-08 09:06:56');
/*!40000 ALTER TABLE `wallet` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wallet_transactions`
--

DROP TABLE IF EXISTS `wallet_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallet_transactions` (
  `wallet_transaction_id` bigint NOT NULL AUTO_INCREMENT,
  `wallet_id` bigint NOT NULL,
  `transaction_type` enum('CREDIT','DEBIT','REFUND') NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `balance_before` decimal(10,2) NOT NULL,
  `balance_after` decimal(10,2) NOT NULL,
  `reference_id` varchar(50) DEFAULT NULL,
  `reference_type` enum('PURCHASE','TOPUP','REFUND','ADJUSTMENT') DEFAULT NULL,
  `description` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`wallet_transaction_id`),
  KEY `idx_wallet_id` (`wallet_id`),
  KEY `idx_transaction_type` (`transaction_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_wallet_trans_wallet_type` (`wallet_id`,`transaction_type`),
  CONSTRAINT `wallet_transactions_ibfk_1` FOREIGN KEY (`wallet_id`) REFERENCES `wallet` (`wallet_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Wallet transaction history';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wallet_transactions`
--

LOCK TABLES `wallet_transactions` WRITE;
/*!40000 ALTER TABLE `wallet_transactions` DISABLE KEYS */;
/*!40000 ALTER TABLE `wallet_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Final view structure for view `vw_cart_summary`
--

/*!50001 DROP VIEW IF EXISTS `vw_cart_summary`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `vw_cart_summary` AS select `c`.`cart_id` AS `cart_id`,`c`.`user_id` AS `user_id`,`u`.`name` AS `user_name`,`u`.`email` AS `email`,count(`ci`.`cart_item_id`) AS `items_count`,coalesce(sum(`ci`.`subtotal`),0) AS `cart_total`,`c`.`updated_at` AS `last_updated`,`c`.`is_active` AS `is_active` from ((`cart` `c` join `users` `u` on((`c`.`user_id` = `u`.`user_id`))) left join `cart_items` `ci` on((`c`.`cart_id` = `ci`.`cart_id`))) group by `c`.`cart_id`,`c`.`user_id`,`u`.`name`,`u`.`email`,`c`.`updated_at`,`c`.`is_active` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `vw_products_full`
--

/*!50001 DROP VIEW IF EXISTS `vw_products_full`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `vw_products_full` AS select `p`.`product_id` AS `product_id`,`p`.`name` AS `product_name`,`p`.`brand` AS `brand`,`c`.`category_name` AS `category_name`,`p`.`description` AS `description`,`p`.`mrp` AS `mrp`,`p`.`selling_price` AS `selling_price`,`p`.`discount_percentage` AS `discount_percentage`,`p`.`unit` AS `unit`,`p`.`stock_quantity` AS `stock_quantity`,`p`.`is_available` AS `is_available`,`r`.`rfid_tag` AS `rfid_tag`,`p`.`barcode` AS `barcode`,`p`.`image_url` AS `image_url` from ((`products` `p` left join `categories` `c` on((`p`.`category_id` = `c`.`category_id`))) left join `rfid_tags` `r` on((`p`.`product_id` = `r`.`product_id`))) where ((`p`.`is_available` = true) and (`r`.`is_active` = true)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `vw_users_wallet`
--

/*!50001 DROP VIEW IF EXISTS `vw_users_wallet`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `vw_users_wallet` AS select `u`.`user_id` AS `user_id`,`u`.`name` AS `name`,`u`.`email` AS `email`,`u`.`phone` AS `phone`,`u`.`biometric_enabled` AS `biometric_enabled`,`u`.`status` AS `status`,`u`.`created_at` AS `created_at`,`u`.`last_login` AS `last_login`,`w`.`balance` AS `wallet_balance`,`w`.`currency` AS `currency` from (`users` `u` left join `wallet` `w` on((`u`.`user_id` = `w`.`user_id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-09 13:54:24
