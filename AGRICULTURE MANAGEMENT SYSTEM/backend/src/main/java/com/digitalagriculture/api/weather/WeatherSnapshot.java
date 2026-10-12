package com.digitalagriculture.api.weather;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "weather_snapshots")
public class WeatherSnapshot {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "forecast_json", nullable = false, columnDefinition = "TEXT")
    private String forecastJson;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected WeatherSnapshot() {
    }

    public WeatherSnapshot(UUID userId, BigDecimal latitude, BigDecimal longitude, String forecastJson) {
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.forecastJson = forecastJson;
        this.fetchedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public String getForecastJson() { return forecastJson; }
    public Instant getFetchedAt() { return fetchedAt; }
}
