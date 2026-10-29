package com.yms.yms.domain.user.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.user.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.username(), request.password());
        return ApiResponse.ok(toResponse(result));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    public ApiResponse<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        AuthService.LoginResult result = authService.refresh(request.refreshToken());
        return ApiResponse.ok(toResponse(result));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(Authentication authentication) {
        return ApiResponse.ok(new MeResponse(
                authentication.getName(),
                authentication.getAuthorities().stream().map(Object::toString).toList()
        ));
    }

    private LoginResponse toResponse(AuthService.LoginResult result) {
        List<String> roles = result.user().getRoles().stream()
                .map(r -> r.getCode())
                .toList();
        return new LoginResponse(result.accessToken(), result.refreshToken(),
                result.user().getUsername(), roles);
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record LoginResponse(String accessToken, String refreshToken, String username, List<String> roles) {
    }

    public record MeResponse(String username, List<String> authorities) {
    }
}
