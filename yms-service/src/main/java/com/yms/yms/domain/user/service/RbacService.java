package com.yms.yms.domain.user.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.user.entity.Permission;
import com.yms.yms.domain.user.entity.Role;
import com.yms.yms.domain.user.entity.User;
import com.yms.yms.domain.user.repository.PermissionRepository;
import com.yms.yms.domain.user.repository.RoleRepository;
import com.yms.yms.domain.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RbacService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RbacService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public void assignRoles(UUID userId, Set<UUID> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException("One or more roles not found", HttpStatus.BAD_REQUEST);
        }
        boolean wasAdmin = user.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getCode()));
        if (wasAdmin && roles.stream().noneMatch(role -> "ADMIN".equals(role.getCode()))) {
            throw new BusinessException("Cannot remove the Admin role", HttpStatus.FORBIDDEN);
        }
        user.setRoles(new HashSet<>(roles));
        userRepository.save(user);
    }

    @Transactional
    public Role updateRolePermissions(UUID roleId, Set<String> permissionCodes) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("Role not found", HttpStatus.NOT_FOUND));
        List<Permission> allPermissions = permissionRepository.findAll();
        Set<String> knownCodes = allPermissions.stream().map(Permission::getCode)
                .collect(java.util.stream.Collectors.toSet());
        if (permissionCodes == null || !knownCodes.containsAll(permissionCodes)) {
            throw new BusinessException("One or more permissions do not exist", HttpStatus.BAD_REQUEST);
        }
        if ("ADMIN".equals(role.getCode())) {
            role.getPermissions().clear();
            role.getPermissions().addAll(allPermissions);
            return roleRepository.saveAndFlush(role);
        }
        if (permissionCodes.contains("*")) {
            throw new BusinessException("Wildcard permission is reserved for Admin", HttpStatus.FORBIDDEN);
        }
        Set<Permission> selectedPermissions = allPermissions.stream()
                .filter(permission -> permissionCodes.contains(permission.getCode()))
                .collect(java.util.stream.Collectors.toSet());
        // Keep Hibernate's managed collection and let it calculate join-table deletes/inserts.
        role.getPermissions().removeIf(permission -> !selectedPermissions.contains(permission));
        role.getPermissions().addAll(selectedPermissions);
        return roleRepository.saveAndFlush(role);
    }

    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    public List<Permission> listPermissions() {
        return permissionRepository.findAll();
    }
}
