package com.digitalagriculture.api.crop;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropRepository extends JpaRepository<Crop, UUID> {
    long countByStatus(String status);
}
