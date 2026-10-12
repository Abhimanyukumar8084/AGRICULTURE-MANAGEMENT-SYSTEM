package com.digitalagriculture.api.market;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class MarketPriceService {
    private static final String SOURCE = "AGMARKNET";
    private final MarketPriceRepository prices;
    private final RestClient http;
    private final String apiKey;
    private final String resourceId;

    public MarketPriceService(
            MarketPriceRepository prices,
            RestClient.Builder restClientBuilder,
            @Value("${app.market-data.api-key:}") String apiKey,
            @Value("${app.market-data.resource-id}") String resourceId) {
        this.prices = prices;
        this.http = restClientBuilder.build();
        this.apiKey = apiKey;
        this.resourceId = resourceId;
    }

    @Transactional(readOnly = true)
    public List<MarketPrice> list(String state, String district) {
        var page = PageRequest.of(0, 500);
        if (state == null || state.isBlank()) return prices.findAllByOrderByArrivalDateDescCommodityAsc(page);
        if (district == null || district.isBlank()) {
            return prices.findByStateIgnoreCaseOrderByArrivalDateDescCommodityAsc(state.trim(), page);
        }
        return prices.findByStateIgnoreCaseAndDistrictIgnoreCaseOrderByArrivalDateDescCommodityAsc(
                state.trim(), district.trim(), page);
    }

    @Transactional
    public RefreshResult refresh(String state, String district) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Market data provider is not configured. Set DATA_GOV_API_KEY on the backend.");
        }
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString("https://api.data.gov.in/resource/" + resourceId)
                .queryParam("api-key", apiKey)
                .queryParam("format", "json")
                .queryParam("limit", 100);
        if (state != null && !state.isBlank()) uri.queryParam("filters[state]", state.trim());
        if (district != null && !district.isBlank()) uri.queryParam("filters[district]", district.trim());

        JsonNode response;
        try {
            response = http.get().uri(uri.build().encode().toUri()).retrieve().body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Market data provider could not be reached. Please try again later.");
        }
        JsonNode records = response == null ? null : response.path("records");
        if (records == null || !records.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Market data provider returned an invalid response.");
        }
        int saved = 0;
        for (JsonNode record : records) {
            MarketPrice price = mapRecord(record);
            MarketPrice existing = prices.findBySourceKey(price.getSourceKey()).orElse(null);
            if (existing == null) prices.save(price);
            else {
                existing.updateFrom(price);
                prices.save(existing);
            }
            saved++;
        }
        return new RefreshResult(saved, Instant.now());
    }

    private MarketPrice mapRecord(JsonNode record) {
        String state = required(record, "state");
        String market = required(record, "market");
        String commodity = required(record, "commodity");
        String arrivalDate = required(record, "arrival_date");
        return new MarketPrice(priceKey(record), state, optional(record, "district"), market, commodity,
                optional(record, "variety"), optional(record, "grade"), arrivalDate,
                amount(record, "min_price"), amount(record, "max_price"), amount(record, "modal_price"), SOURCE);
    }

    private static String required(JsonNode node, String field) {
        String value = optional(node, field);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Market data provider returned an incomplete record.");
        }
        return value;
    }

    private static String optional(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        return value.isEmpty() ? null : value;
    }

    private static BigDecimal amount(JsonNode node, String field) {
        String value = required(node, field).replace(",", "");
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Market data provider returned an invalid price.");
        }
    }

    private static String priceKey(JsonNode record) {
        String key = List.of("state", "district", "market", "commodity", "variety", "grade", "arrival_date")
                .stream().map(field -> {
                    String value = optional(record, field);
                    return value == null ? "" : value.toLowerCase(Locale.ROOT);
                }).collect(java.util.stream.Collectors.joining("|"));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    public record RefreshResult(int recordsSaved, Instant refreshedAt) {
    }
}
