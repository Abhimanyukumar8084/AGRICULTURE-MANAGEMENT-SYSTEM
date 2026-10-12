package com.digitalagriculture.api.web;

import com.digitalagriculture.api.user.FarmerProvisioningService;
import com.digitalagriculture.api.web.dto.AdminFarmerRequest;
import com.digitalagriculture.api.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/farmers")
public class AdminFarmerController {
    private final FarmerProvisioningService farmers;

    public AdminFarmerController(FarmerProvisioningService farmers) {
        this.farmers = farmers;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody AdminFarmerRequest request) {
        return UserResponse.from(farmers.create(request));
    }
}
