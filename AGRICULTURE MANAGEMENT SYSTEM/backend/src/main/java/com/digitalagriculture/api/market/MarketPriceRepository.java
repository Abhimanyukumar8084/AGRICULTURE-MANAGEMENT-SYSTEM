package com.digitalagriculture.api.market;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, UUID> {
    List<MarketPrice> findAllByOrderByArrivalDateDescCommodityAsc(Pageable pageable);
    List<MarketPrice> findByStateIgnoreCaseOrderByArrivalDateDescCommodityAsc(String state, Pageable pageable);
    List<MarketPrice> findByStateIgnoreCaseAndDistrictIgnoreCaseOrderByArrivalDateDescCommodityAsc(
            String state, String district, Pageable pageable);
    Optional<MarketPrice> findBySourceKey(String sourceKey);
}
