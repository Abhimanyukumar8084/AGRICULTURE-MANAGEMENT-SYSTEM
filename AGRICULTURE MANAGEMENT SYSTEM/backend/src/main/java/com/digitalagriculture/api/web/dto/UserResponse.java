package com.digitalagriculture.api.web.dto;

import com.digitalagriculture.api.user.User;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

public record UserResponse(UUID id, String firstName, String lastName, String email, String phone, String role,
                           String location, String farmName, String farmArea, String soilType,
                           String irrigationMethod, Instant createdAt, String username, String gender, String state,
                           String district, String village, String pinCode, String mainCrop, String farmingType,
                           BigDecimal landArea, String landUnit, String accountStatus, BigDecimal latitude,
                           BigDecimal longitude, boolean notifyWeather, boolean notifyMarket,
                           boolean notifyActivities) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getPhone(), user.getRole().name().toLowerCase(java.util.Locale.ROOT),
                user.getLocation(), user.getFarmName(), user.getFarmArea(), user.getSoilType(),
                user.getIrrigationMethod(), user.getCreatedAt(), user.getFarmUsername(), user.getGender(), user.getState(),
                user.getDistrict(), user.getVillage(), user.getPinCode(), user.getMainCrop(), user.getFarmingType(),
                user.getLandArea(), user.getLandUnit(), user.getAccountStatus(), user.getLatitude(), user.getLongitude(),
                user.isNotifyWeather(), user.isNotifyMarket(), user.isNotifyActivities());
    }
}
