package com.digitalagriculture.api.consultation;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {
    List<Consultation> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId);
    List<Consultation> findByExpertIdOrderByCreatedAtDesc(UUID expertId);
    List<Consultation> findByExpertIdIsNullAndStatusOrderByCreatedAtDesc(ConsultationStatus status);
    long countByStatus(ConsultationStatus status);
}
