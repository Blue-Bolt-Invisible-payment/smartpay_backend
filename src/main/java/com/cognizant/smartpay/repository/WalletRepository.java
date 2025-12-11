package com.cognizant.smartpay.repository;

import com.cognizant.smartpay.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Wallet entity
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * Find wallet by user ID
     */
    Optional<Wallet> findByUserId(Long userId);

    /**
     * Check if wallet exists for user
     */
    boolean existsByUserId(Long userId);
}
