package com.digitalagriculture.api.weather;

import com.digitalagriculture.api.user.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WeatherService {
    private final WeatherSnapshotRepository snapshots;
    private final RestClient http;
    private final ObjectMapper objectMapper;

    public WeatherService(WeatherSnapshotRepository snapshots, RestClient.Builder restClientBuilder,
                          ObjectMapper objectMapper) {
        this.snapshots = snapshots;
        this.http = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WeatherSnapshotResponse refresh(User user) {
        if (user.getLatitude() == null || user.getLongitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Save device location before refreshing the forecast.");
        }
        var uri = UriComponentsBuilder.fromUriString("https://api.open-meteo.com/v1/forecast")
                .queryParam("latitude", user.getLatitude())
                .queryParam("longitude", user.getLongitude())
                .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m")
                .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                .queryParam("timezone", "auto")
                .queryParam("forecast_days", 7)
                .build()
                .encode()
                .toUri();
        JsonNode forecast;
        try {
            forecast = http.get().uri(uri).retrieve().body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Live weather provider could not be reached. Please try again later.");
        }
        if (forecast == null || forecast.path("current").isMissingNode() || !forecast.path("daily").path("time").isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Live weather provider returned an invalid forecast.");
        }
        try {
            WeatherSnapshot snapshot = snapshots.save(new WeatherSnapshot(user.getId(), user.getLatitude(),
                    user.getLongitude(), objectMapper.writeValueAsString(forecast)));
            return response(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to store the weather provider response.", exception);
        }
    }

    @Transactional(readOnly = true)
    public WeatherSnapshotResponse current(UUID userId) {
        WeatherSnapshot snapshot = snapshots.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No forecast is stored yet. Refresh the weather page to load one."));
        return response(snapshot);
    }

    private WeatherSnapshotResponse response(WeatherSnapshot snapshot) {
        try {
            JsonNode forecast = objectMapper.readTree(snapshot.getForecastJson());
            return new WeatherSnapshotResponse(snapshot.getLatitude(), snapshot.getLongitude(),
                    snapshot.getFetchedAt(), forecast);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("The stored weather forecast is invalid.", exception);
        }
    }

    public record WeatherSnapshotResponse(
            java.math.BigDecimal latitude,
            java.math.BigDecimal longitude,
            Instant fetchedAt,
            JsonNode forecast) {
    }
}
