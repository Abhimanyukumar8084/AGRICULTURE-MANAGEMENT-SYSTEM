package com.digitalagriculture.api.weather;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherSnapshotRepository extends JpaRepository<WeatherSnapshot, UUID> {
}
