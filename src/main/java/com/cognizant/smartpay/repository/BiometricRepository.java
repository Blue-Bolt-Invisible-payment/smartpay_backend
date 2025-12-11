package com.cognizant.smartpay.repository;

import com.cognizant.smartpay.entity.Biometric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Biometric entity
 */
@Repository
public interface BiometricRepository extends JpaRepository<Biometric, Long> {

    /**
     * Find biometric by credential ID and active status
     */
    Optional<Biometric> findByCredentialIdAndIsActive(String credentialId, Boolean isActive);

    /**
     * Find all biometrics by user ID and active status
     */
    List<Biometric> findByUserIdAndIsActive(Long userId, Boolean isActive);

    /**
     * Find all active biometrics
     */
    List<Biometric> findAllByIsActive(Boolean isActive);

    /**
     * Find biometric by user ID, device type and active status
     */
    Optional<Biometric> findByUserIdAndDeviceTypeAndIsActive(Long userId, String deviceType, Boolean isActive);

    /**
     * Check if user has active biometric
     */
    boolean existsByUserIdAndIsActive(Long userId, Boolean isActive);

    /**
     * Find biometric with user data by credential ID
     */
    @Query("SELECT b FROM Biometric b JOIN FETCH b.user WHERE b.credentialId = :credentialId AND b.isActive = true")
    Optional<Biometric> findByCredentialIdWithUser(@Param("credentialId") String credentialId);
}
