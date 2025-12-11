package com.cognizant.smartpay.repository;

import com.cognizant.smartpay.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Product entity
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find products by category
     */
    List<Product> findByCategory(String category);

    /**
     * Find products with available stock
     */
    List<Product> findByStockQuantityGreaterThan(Integer quantity);
}
