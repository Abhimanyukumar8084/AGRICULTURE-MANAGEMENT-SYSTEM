package com.digitalagriculture.api.market;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "market_prices")
public class MarketPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_key", nullable = false, unique = true, length = 64)
    private String sourceKey;

    @Column(nullable = false, length = 80)
    private String state;

    @Column(length = 80)
    private String district;

    @Column(nullable = false, length = 120)
    private String market;

    @Column(nullable = false, length = 120)
    private String commodity;

    @Column(length = 120)
    private String variety;

    @Column(length = 80)
    private String grade;

    @Column(name = "arrival_date", nullable = false, length = 24)
    private String arrivalDate;

    @Column(name = "min_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "modal_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal modalPrice;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected MarketPrice() {
    }

    public MarketPrice(String sourceKey, String state, String district, String market, String commodity,
                       String variety, String grade, String arrivalDate, BigDecimal minPrice,
                       BigDecimal maxPrice, BigDecimal modalPrice, String source) {
        this.sourceKey = sourceKey;
        this.state = state;
        this.district = district;
        this.market = market;
        this.commodity = commodity;
        this.variety = variety;
        this.grade = grade;
        this.arrivalDate = arrivalDate;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.modalPrice = modalPrice;
        this.source = source;
    }

    public void updateFrom(MarketPrice latest) {
        state = latest.state;
        district = latest.district;
        market = latest.market;
        commodity = latest.commodity;
        variety = latest.variety;
        grade = latest.grade;
        arrivalDate = latest.arrivalDate;
        minPrice = latest.minPrice;
        maxPrice = latest.maxPrice;
        modalPrice = latest.modalPrice;
        source = latest.source;
        fetchedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        fetchedAt = Instant.now();
    }

    public UUID getId() { return id; }
    @JsonIgnore
    public String getSourceKey() { return sourceKey; }
    public String getState() { return state; }
    public String getDistrict() { return district; }
    public String getMarket() { return market; }
    public String getCommodity() { return commodity; }
    public String getVariety() { return variety; }
    public String getGrade() { return grade; }
    public String getArrivalDate() { return arrivalDate; }
    public BigDecimal getMinPrice() { return minPrice; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public BigDecimal getModalPrice() { return modalPrice; }
    public String getSource() { return source; }
    public Instant getFetchedAt() { return fetchedAt; }
}
