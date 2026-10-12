package com.digitalagriculture.api.record;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmRecordRepository extends JpaRepository<FarmRecord, UUID> {
    List<FarmRecord> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    List<FarmRecord> findByOwnerIdAndKindOrderByCreatedAtDesc(UUID ownerId, RecordKind kind);
    List<FarmRecord> findByKindOrderByCreatedAtDesc(RecordKind kind);
    List<FarmRecord> findByKindAndStatusOrderByCreatedAtDesc(RecordKind kind, String status);
    long countByKind(RecordKind kind);
    long countByKindAndStatus(RecordKind kind, String status);
}
