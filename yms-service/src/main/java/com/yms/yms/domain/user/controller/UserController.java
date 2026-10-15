package com.yms.yms.domain.user.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.user.entity.User;
import com.yms.yms.domain.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<UserView>> list() {
        return ApiResponse.ok(userService.findAll().stream().map(UserView::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserView> get(@PathVariable UUID id) {
        return ApiResponse.ok(UserView.from(userService.findById(id)));
    }

    @PostMapping
    public ApiResponse<UserView> create(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.create(request.username(), request.password(),
                request.roles(), request.enabled() == null || request.enabled());
        return ApiResponse.ok(UserView.from(user));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserView> update(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        User user = userService.update(id, request.enabled(), request.roles(), request.password());
        return ApiResponse.ok(UserView.from(user));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ApiResponse.ok(null);
    }

    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank String password,
            Set<String> roles,
            Boolean enabled
    ) {
    }

    public record UpdateUserRequest(Boolean enabled, Set<String> roles, String password) {
    }

    public record UserView(UUID id, String username, boolean enabled, int failedLoginAttempts,
                           List<String> roles) {
        static UserView from(User u) {
            return new UserView(
                    u.getId(),
                    u.getUsername(),
                    u.isEnabled(),
                    u.getFailedLoginAttempts(),
                    u.getRoles().stream().map(r -> r.getCode()).toList()
            );
        }
    }
}
