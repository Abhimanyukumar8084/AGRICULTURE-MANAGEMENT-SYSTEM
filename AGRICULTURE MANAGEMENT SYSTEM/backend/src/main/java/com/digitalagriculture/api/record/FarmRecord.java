package com.digitalagriculture.api.record;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "farm_records")
public class FarmRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RecordKind kind;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 200)
    private String category;

    @Column(length = 3000)
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(length = 32)
    private String unit;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 120)
    private String location;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    private LocalDate dueDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected FarmRecord() {
    }

    public FarmRecord(UUID ownerId, RecordKind kind, String title, String category, String description,
                      BigDecimal quantity, String unit, BigDecimal price, String location,
                      String status, LocalDate dueDate) {
        this.ownerId = ownerId;
        this.kind = kind;
        this.title = title.trim();
        this.category = clean(category);
        this.description = clean(description);
        this.quantity = quantity;
        this.unit = clean(unit);
        this.price = price;
        this.location = clean(location);
        this.status = status == null || status.isBlank() ? "ACTIVE" : status.trim().toUpperCase(java.util.Locale.ROOT);
        this.dueDate = dueDate;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public RecordKind getKind() { return kind; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public BigDecimal getPrice() { return price; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }
    public Instant getCreatedAt() { return createdAt; }
    public void setStatus(String status) {
        this.status = status == null || status.isBlank() ? "ACTIVE" : status.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
