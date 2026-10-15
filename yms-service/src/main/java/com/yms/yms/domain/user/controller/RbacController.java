package com.yms.yms.domain.user.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.user.entity.Permission;
import com.yms.yms.domain.user.service.RbacService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/rbac")
@PreAuthorize("hasRole('ADMIN')")
public class RbacController {

    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/permissions")
    public ApiResponse<List<Permission>> permissions() {
        return ApiResponse.ok(rbacService.listPermissions());
    }

    @PostMapping("/users/{userId}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable UUID userId, @RequestBody AssignRolesRequest request) {
        rbacService.assignRoles(userId, request.roleIds());
        return ApiResponse.ok(null);
    }

    public record AssignRolesRequest(Set<UUID> roleIds) {
    }
}
