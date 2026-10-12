package com.digitalagriculture.api.web;

import com.digitalagriculture.api.market.MarketPrice;
import com.digitalagriculture.api.market.MarketPriceService;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market/prices")
public class MarketPriceController {
    private final MarketPriceService prices;

    public MarketPriceController(MarketPriceService prices) {
        this.prices = prices;
    }

    @GetMapping
    public List<MarketPrice> list(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district) {
        return prices.list(state, district);
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> refresh(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district) {
        MarketPriceService.RefreshResult result = prices.refresh(state, district);
        return Map.of("recordsSaved", result.recordsSaved(), "refreshedAt", result.refreshedAt());
    }
}
