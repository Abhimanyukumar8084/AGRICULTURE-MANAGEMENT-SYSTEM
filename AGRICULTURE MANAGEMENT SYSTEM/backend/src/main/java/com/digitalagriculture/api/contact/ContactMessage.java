package com.digitalagriculture.api.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contact_messages")
public class ContactMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 40)
    private String topic;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ContactMessage() {
    }

    public ContactMessage(String name, String email, String topic, String message) {
        this.name = name;
        this.email = email;
        this.topic = topic;
        this.message = message;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
}
