package com.digitalagriculture.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(length = 24)
    private String phone;

    @Column(length = 120)
    private String location;

    @Column(length = 120)
    private String farmName;

    @Column(length = 40)
    private String farmArea;

    @Column(length = 60)
    private String soilType;

    @Column(length = 60)
    private String irrigationMethod;

    @Column(unique = true, length = 40)
    private String username;

    @Column(length = 16)
    private String gender;

    @Column(length = 80)
    private String state;

    @Column(length = 80)
    private String district;

    @Column(length = 120)
    private String village;

    @Column(name = "pin_code", length = 10)
    private String pinCode;

    @Column(name = "main_crop", length = 80)
    private String mainCrop;

    @Column(name = "farming_type", length = 40)
    private String farmingType;

    @Column(name = "land_area", precision = 10, scale = 2)
    private BigDecimal landArea;

    @Column(name = "land_unit", length = 24)
    private String landUnit;

    @Column(name = "account_status", nullable = false, length = 16)
    private String accountStatus = "ACTIVE";

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "notify_weather", nullable = false)
    private boolean notifyWeather = true;

    @Column(name = "notify_market", nullable = false)
    private boolean notifyMarket = true;

    @Column(name = "notify_activities", nullable = false)
    private boolean notifyActivities = true;

    @Column(nullable = false)
    private String passwordHash;

    @Column(name = "password_reset_token", length = 128)
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private Instant passwordResetExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountRole role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String firstName, String lastName, String email, String phone, String passwordHash, AccountRole role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email.toLowerCase(java.util.Locale.ROOT);
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getLocation() { return location; }
    public String getFarmName() { return farmName; }
    public String getFarmArea() { return farmArea; }
    public String getSoilType() { return soilType; }
    public String getIrrigationMethod() { return irrigationMethod; }
    public String getFarmUsername() { return username; }
    public String getGender() { return gender; }
    public String getState() { return state; }
    public String getDistrict() { return district; }
    public String getVillage() { return village; }
    public String getPinCode() { return pinCode; }
    public String getMainCrop() { return mainCrop; }
    public String getFarmingType() { return farmingType; }
    public BigDecimal getLandArea() { return landArea; }
    public String getLandUnit() { return landUnit; }
    public String getAccountStatus() { return accountStatus; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public boolean isNotifyWeather() { return notifyWeather; }
    public boolean isNotifyMarket() { return notifyMarket; }
    public boolean isNotifyActivities() { return notifyActivities; }
    public String getPasswordResetToken() { return passwordResetToken; }
    public Instant getPasswordResetExpiresAt() { return passwordResetExpiresAt; }
    public AccountRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setPasswordResetToken(String token, Instant expiresAt) {
        this.passwordResetToken = token == null || token.isBlank() ? null : token.trim();
        this.passwordResetExpiresAt = expiresAt;
    }
    public void clearPasswordResetToken() {
        this.passwordResetToken = null;
        this.passwordResetExpiresAt = null;
    }
    public void updateProfile(String firstName, String lastName, String phone, String location, String farmName,
                              String farmArea, String soilType, String irrigationMethod) {
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        if (phone != null) this.phone = clean(phone);
        if (location != null) this.location = clean(location);
        if (farmName != null) this.farmName = clean(farmName);
        if (farmArea != null) this.farmArea = clean(farmArea);
        if (soilType != null) this.soilType = clean(soilType);
        if (irrigationMethod != null) this.irrigationMethod = clean(irrigationMethod);
    }
    public void setFarmerDetails(String username, String gender, String state, String district, String village,
                                 String pinCode, String mainCrop, String farmingType, BigDecimal landArea,
                                 String landUnit, String accountStatus) {
        this.username = clean(username);
        this.gender = clean(gender);
        this.state = clean(state);
        this.district = clean(district);
        this.village = clean(village);
        this.pinCode = clean(pinCode);
        this.mainCrop = clean(mainCrop);
        this.farmingType = clean(farmingType);
        this.landArea = landArea;
        this.landUnit = clean(landUnit);
        String normalizedStatus = accountStatus == null ? null : accountStatus.trim().toUpperCase(java.util.Locale.ROOT);
        this.accountStatus = normalizedStatus == null || normalizedStatus.isBlank() ? "ACTIVE" : normalizedStatus;
        this.enabled = "ACTIVE".equals(this.accountStatus);
    }
    public void updateLocation(BigDecimal latitude, BigDecimal longitude, String location,
                               String state, String district, String village) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.location = clean(location);
        this.state = clean(state);
        this.district = clean(district);
        this.village = clean(village);
    }
    public void updateNotifications(Boolean weather, Boolean market, Boolean activities) {
        if (weather != null) notifyWeather = weather;
        if (market != null) notifyMarket = market;
        if (activities != null) notifyActivities = activities;
    }
    public void updateMarketLocation(String state, String district) {
        if (state != null) this.state = clean(state);
        if (district != null) this.district = clean(district);
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return enabled; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
