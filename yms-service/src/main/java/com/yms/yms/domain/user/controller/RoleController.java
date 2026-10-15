package com.yms.yms.domain.user.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.user.entity.Role;
import com.yms.yms.domain.user.repository.RoleRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
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
