package com.digitalagriculture.api.consultation;

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
import java.util.UUID;

@Entity
@Table(name = "consultations")
public class Consultation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID farmerId;

    private UUID expertId;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false, length = 100)
    private String crop;

    @Column(nullable = false, length = 3000)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConsultationStatus status = ConsultationStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Consultation() {
    }

    public Consultation(UUID farmerId, UUID expertId, String topic, String crop, String question) {
        this.farmerId = farmerId;
        this.expertId = expertId;
        this.topic = topic.trim();
        this.crop = crop.trim();
        this.question = question.trim();
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getFarmerId() { return farmerId; }
    public UUID getExpertId() { return expertId; }
    public String getTopic() { return topic; }
    public String getCrop() { return crop; }
    public String getQuestion() { return question; }
    public ConsultationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setStatus(ConsultationStatus status) { this.status = status; }
    public void setExpertId(UUID expertId) { this.expertId = expertId; }
}
