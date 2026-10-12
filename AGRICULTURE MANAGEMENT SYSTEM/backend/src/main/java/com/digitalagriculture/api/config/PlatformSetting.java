package com.digitalagriculture.api.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "platform_settings")
public class PlatformSetting {
    @Id
    @Column(name = "setting_key", length = 64)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 160)
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlatformSetting() {
    }

    public PlatformSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
