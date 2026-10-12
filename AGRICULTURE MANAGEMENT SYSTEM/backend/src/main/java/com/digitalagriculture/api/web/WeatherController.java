package com.digitalagriculture.api.web;

import com.digitalagriculture.api.user.User;
import com.digitalagriculture.api.weather.WeatherService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    private final WeatherService weather;

    public WeatherController(WeatherService weather) {
        this.weather = weather;
    }

    @GetMapping("/current")
    public WeatherService.WeatherSnapshotResponse current(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return weather.current(user.getId());
    }

    @PostMapping("/refresh")
    public WeatherService.WeatherSnapshotResponse refresh(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return weather.refresh(user);
    }
}
