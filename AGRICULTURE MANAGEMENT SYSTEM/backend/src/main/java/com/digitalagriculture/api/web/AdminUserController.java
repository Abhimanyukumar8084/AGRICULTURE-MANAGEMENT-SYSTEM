package com.digitalagriculture.api.web;

import com.digitalagriculture.api.user.AccountRole;
import com.digitalagriculture.api.user.User;
import com.digitalagriculture.api.user.UserRepository;
import com.digitalagriculture.api.web.dto.ExpertCreateRequest;
import com.digitalagriculture.api.web.dto.UserResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    public Page<UserResponse> users(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return users.findAll(pageable).map(UserResponse::from);
    }

    @PostMapping("/experts")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createExpert(@Valid @RequestBody ExpertCreateRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be no more than 72 UTF-8 bytes.");
        }
        String email = request.email() == null ? null : request.email().trim();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        User expert = users.save(new User(request.firstName().trim(), request.lastName().trim(),
                email,
                request.phone() == null ? null : request.phone().trim(),
                passwordEncoder.encode(request.password()), AccountRole.EXPERT));
        return UserResponse.from(expert);
    }
}
