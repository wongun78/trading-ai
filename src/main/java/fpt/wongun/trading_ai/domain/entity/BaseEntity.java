package fpt.wongun.trading_ai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base entity with auditing fields and soft delete support.
 * All entities should extend this class for consistent auditing and deletion tracking.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(length = 100, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(length = 100)
    private String lastModifiedBy;

    @Version
    private Long version;  // Optimistic locking

    @Column(nullable = false)
    private Boolean deleted = false;

    private Instant deletedAt;

    @Column(length = 100)
    private String deletedBy;

    /**
     * Soft delete the entity
     */
    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.deletedBy = deletedBy;
    }

    /**
     * Check if entity is deleted
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }
}
