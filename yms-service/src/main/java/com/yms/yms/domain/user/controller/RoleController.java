package com.yms.yms.domain.user.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.user.entity.Role;
import com.yms.yms.domain.user.repository.RoleRepository;
import com.yms.yms.domain.user.service.RbacService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class RoleController {

    private final RoleRepository roleRepository;
    private final RbacService rbacService;

    public RoleController(RoleRepository roleRepository, RbacService rbacService) {
        this.roleRepository = roleRepository;
        this.rbacService = rbacService;
    }

    @GetMapping
    public ApiResponse<List<RoleView>> list() {
        return ApiResponse.ok(roleRepository.findAll().stream().map(RoleView::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleView> get(@PathVariable UUID id) {
        Role role = roleRepository.findById(id).orElseThrow();
        return ApiResponse.ok(RoleView.from(role));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RoleView> updatePermissions(@PathVariable UUID id,
                                                    @RequestBody UpdateRolePermissionsRequest request) {
        return ApiResponse.ok(RoleView.from(rbacService.updateRolePermissions(id, request.permissionCodes())));
    }

    public record UpdateRolePermissionsRequest(java.util.Set<String> permissionCodes) { }

    public record RoleView(UUID id, String code, String name, List<String> permissions) {
        static RoleView from(Role role) {
            return new RoleView(
                    role.getId(),
                    role.getCode(),
                    role.getName(),
                    role.getPermissions().stream().map(p -> p.getCode()).toList()
            );
        }
    }
}
