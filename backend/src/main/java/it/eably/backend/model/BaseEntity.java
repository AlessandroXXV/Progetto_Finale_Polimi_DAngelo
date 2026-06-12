package it.eably.backend.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base entity class implementing common fields and behavior for all domain entities.
 * 
 * This class demonstrates strict OOP principles with:
 * - Abstraction: Abstract class with template method pattern
 * - Encapsulation: Protected fields with controlled access
 * - Inheritance: Base for all domain entities
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Primary key for all entities.
     * Uses IDENTITY strategy for PostgreSQL compatibility.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Timestamp of entity creation.
     * Automatically populated by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp of last entity modification.
     * Automatically updated by JPA auditing.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Version field for optimistic locking.
     * Prevents lost updates in concurrent scenarios.
     * 
     */
    @Version
    @Column(name = "version")
    private Long version;
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    /**
     * JPA lifecycle callback to set timestamps before persisting.
     * Called automatically by JPA before INSERT operations.
     */
    @PrePersist
    protected final void onCreate() {
        validate();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * JPA lifecycle callback to update timestamp before updating.
     * Called automatically by JPA before UPDATE operations.
     */
    @PreUpdate
    protected final void onUpdate() {
        validate();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Hook method for subclass-specific validation. Called automatically before INSERT and UPDATE.
     *
     * Template Method Pattern: BaseEntity defines WHEN validation runs (onCreate/onUpdate);
     * subclasses define WHAT to validate.
     *
     * @throws it.eably.backend.exception.ValidationException if validation fails
     */
    public abstract void validate();
    
    /**
     * Checks if this entity is new (not yet persisted).
     * 
     * @return true if the entity has no ID assigned yet
     */
    @Transient
    public boolean isNew() {
        return this.id == null;
    }
    
    /**
     * Equals implementation based on ID for persisted entities.
     * 
     * @param o object to compare
     * @return true if entities have the same ID and class
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        
        // For new entities, use identity equality
        if (id == null || that.id == null) {
            return false;
        }
        
        // For persisted entities, compare by ID
        return Objects.equals(id, that.id);
    }
    
    /**
     * HashCode implementation based on ID for persisted entities.
     * 
     * @return hash code based on ID or class hash for new entities
     */
    @Override
    public int hashCode() {
        // Use class hash for new entities to maintain consistency
        return id != null ? Objects.hash(id) : getClass().hashCode();
    }
    
    /**
     * String representation for debugging and logging.
     * 
     * @return string representation of the entity
     */
    @Override
    public String toString() {
        return String.format("%s[id=%d, createdAt=%s, updatedAt=%s, version=%d]",
                getClass().getSimpleName(),
                id,
                createdAt,
                updatedAt,
                version);
    }
}
