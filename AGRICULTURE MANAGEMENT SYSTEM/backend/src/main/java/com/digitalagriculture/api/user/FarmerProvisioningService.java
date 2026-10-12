package com.digitalagriculture.api.user;

import com.digitalagriculture.api.web.ApiException;
import com.digitalagriculture.api.web.dto.AdminFarmerRequest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmerProvisioningService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public FarmerProvisioningService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(AdminFarmerRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be no more than 72 UTF-8 bytes.");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String username = request.username().trim();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "That username is already in use.");
        }

        String[] name = request.fullName().trim().split("\\s+", 2);
        String location = String.join(", ", java.util.stream.Stream.of(request.village(), request.district(), request.state())
                .filter(value -> value != null && !value.isBlank()).map(String::trim).toList());
        String landArea = request.landArea() == null ? null
                : request.landArea().stripTrailingZeros().toPlainString();
        String landUnit = request.landUnit() == null || request.landUnit().isBlank() ? "Acres" : request.landUnit().trim();
        User farmer = new User(name[0], name.length > 1 ? name[1] : "", email, request.phone().trim(),
                passwordEncoder.encode(request.password()), AccountRole.FARMER);
        farmer.updateProfile(name[0], name.length > 1 ? name[1] : "", request.phone().trim(), location,
                null, landArea == null ? null : landArea + " " + landUnit, null, null);
        farmer.setFarmerDetails(username, request.gender(), request.state(), request.district(), request.village(),
                request.pinCode(), request.mainCrop(), request.farmingType(), request.landArea(), landUnit,
                request.status().trim().toUpperCase(Locale.ROOT));
        return users.save(farmer);
    }
}
