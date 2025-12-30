package com.cognizant.smartpay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Biometric Entity - Stores biometric authentication data
 */
@Entity
@Table(name = "biometrics", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_credential_id", columnList = "credential_id"),
    @Index(name = "idx_device_type", columnList = "device_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Biometric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "biometric_id")
    private Long biometricId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "fingerprint_hash", nullable = false, length = 512)
    private String fingerprintHash;

    @Lob
    @Column(name = "fingerprint_template", nullable = false, columnDefinition = "BLOB")
    private byte[] fingerprintTemplate;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "enrollment_method", length = 50)
    private String enrollmentMethod;

    @Column(name = "credential_id", length = 512)
    private String credentialId;

    @Lob
    @Column(name = "public_key", columnDefinition = "BLOB")
    private byte[] publicKey;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "verification_count", nullable = false)
    private Integer verificationCount = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        enrolledAt = LocalDateTime.now();
    }
}
