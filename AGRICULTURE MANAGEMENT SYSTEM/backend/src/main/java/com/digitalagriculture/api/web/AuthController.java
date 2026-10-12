package com.digitalagriculture.api.web;

import com.digitalagriculture.api.user.AccountRole;
import com.digitalagriculture.api.user.AuthService;
import com.digitalagriculture.api.user.User;
import com.digitalagriculture.api.user.UserRepository;
import com.digitalagriculture.api.web.dto.LoginRequest;
import com.digitalagriculture.api.web.dto.ForgotPasswordRequest;
import com.digitalagriculture.api.web.dto.LocationUpdateRequest;
import com.digitalagriculture.api.web.dto.PasswordResetRequest;
import com.digitalagriculture.api.web.dto.ProfileUpdateRequest;
import com.digitalagriculture.api.web.dto.RegisterRequest;
import com.digitalagriculture.api.web.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final UserRepository users;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository,
            CsrfTokenRepository csrfTokenRepository,
            UserRepository users) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.users = users;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.loadDeferredToken(request, response).get();
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(authService.register(request));
    }

    @PostMapping("/auth/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim(), request.password()));
        User user = (User) authentication.getPrincipal();
        String requestedAccountType = request.accountType() == null ? null : request.accountType().trim();
        if (requestedAccountType != null && !requestedAccountType.equalsIgnoreCase("user")) {
            AccountRole expected;
            try {
                expected = AccountRole.valueOf(requestedAccountType.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown account type.");
            }
            if (user.getRole() != expected) {
                throw new ApiException(HttpStatus.FORBIDDEN, "This account cannot use the selected login type.");
            }
        } else if (user.getRole() == AccountRole.ADMIN || user.getRole() == AccountRole.EXPERT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Select the matching account type to sign in.");
        }
        servletRequest.getSession(true);
        servletRequest.changeSessionId();
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, servletRequest, servletResponse);
        return UserResponse.from(user);
    }

    @PostMapping("/auth/password/forgot")
    public Map<String, Object> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String resetToken = authService.requestPasswordReset(request.email());
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("message", "If an account exists for that email, a password reset link has been created.");
        if (resetToken != null) {
            response.put("resetToken", resetToken);
        }
        return response;
    }

    @PostMapping("/auth/password/reset")
    public Map<String, String> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request.token(), request.password());
        return Map.of("message", "Your password has been updated successfully.");
    }

    @PostMapping("/auth/logout")
    public Map<String, String> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        csrfTokenRepository.saveToken(null, request, response);
        return Map.of("message", "Signed out successfully.");
    }

    @GetMapping("/auth/me")
    public UserResponse me(Authentication authentication) {
        return UserResponse.from((User) authentication.getPrincipal());
    }

    @PatchMapping("/auth/me")
    public UserResponse updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        user.updateProfile(request.firstName(), request.lastName(), request.phone(), request.location(),
                request.farmName(), request.farmArea(), request.soilType(), request.irrigationMethod());
        user.updateMarketLocation(request.state(), request.district());
        user.updateNotifications(request.notifyWeather(), request.notifyMarket(), request.notifyActivities());
        return UserResponse.from(users.save(user));
    }

    @PatchMapping("/auth/me/location")
    public UserResponse updateLocation(
            @Valid @RequestBody LocationUpdateRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        user.updateLocation(request.latitude(), request.longitude(), request.location(),
                request.state(), request.district(), request.village());
        return UserResponse.from(users.save(user));
    }
}
