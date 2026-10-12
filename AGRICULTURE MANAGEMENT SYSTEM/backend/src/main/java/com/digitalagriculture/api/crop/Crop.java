package com.digitalagriculture.api.crop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crops", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "variety"}))
public class Crop {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 24)
    private String season;

    @Column(nullable = false, length = 100)
    private String variety = "";

    @Column(length = 80)
    private String growingDuration;

    @Column(length = 16)
    private String waterRequirement;

    @Column(length = 60)
    private String soilType;

    @Column(length = 80)
    private String temperatureRange;

    @Column(length = 2000)
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal averagePrice;

    @Column(length = 24)
    private String priceUnit;

    @Column(precision = 12, scale = 2)
    private BigDecimal msp;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Crop() {
    }

    public Crop(String name, String type, String season, String variety, String growingDuration,
                String waterRequirement, String soilType, String temperatureRange, String description,
                BigDecimal averagePrice, String priceUnit, BigDecimal msp, String status) {
        this.name = name.trim();
        this.type = type;
        this.season = season;
        this.variety = variety == null ? "" : variety.trim();
        this.growingDuration = growingDuration;
        this.waterRequirement = waterRequirement;
        this.soilType = soilType;
        this.temperatureRange = temperatureRange;
        this.description = description;
        this.averagePrice = averagePrice;
        this.priceUnit = priceUnit;
        this.msp = msp;
        this.status = status == null || status.isBlank() ? "ACTIVE" : status.trim().toUpperCase(java.util.Locale.ROOT);
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getSeason() { return season; }
    public String getVariety() { return variety; }
    public String getGrowingDuration() { return growingDuration; }
    public String getWaterRequirement() { return waterRequirement; }
    public String getSoilType() { return soilType; }
    public String getTemperatureRange() { return temperatureRange; }
    public String getDescription() { return description; }
    public BigDecimal getAveragePrice() { return averagePrice; }
    public String getPriceUnit() { return priceUnit; }
    public BigDecimal getMsp() { return msp; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void updateFrom(Crop crop) {
        name = crop.name;
        type = crop.type;
        season = crop.season;
        variety = crop.variety;
        growingDuration = crop.growingDuration;
        waterRequirement = crop.waterRequirement;
        soilType = crop.soilType;
        temperatureRange = crop.temperatureRange;
        description = crop.description;
        averagePrice = crop.averagePrice;
        priceUnit = crop.priceUnit;
        msp = crop.msp;
        status = crop.status;
    }
}
