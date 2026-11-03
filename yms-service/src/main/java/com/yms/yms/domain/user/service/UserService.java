package com.yms.yms.domain.user.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.user.entity.Permission;
import com.yms.yms.domain.user.entity.Role;
import com.yms.yms.domain.user.entity.User;
import com.yms.yms.domain.user.entity.UserPermission;
import com.yms.yms.domain.user.repository.PermissionRepository;
import com.yms.yms.domain.user.repository.RoleRepository;
import com.yms.yms.domain.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionRepository permissionRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissionRepository = permissionRepository;
    }

    public List<User> findAll() { return userRepository.findAll(); }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public User create(String username, String rawPassword, Set<String> roleCodes,
                       Set<String> effectivePermissionCodes, boolean enabled) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Username already exists", HttpStatus.CONFLICT);
        }
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEnabled(enabled);
        Set<Role> roles = resolveRoles(roleCodes);
        user.setRoles(roles);
        user.setPermissionOverrides(resolveOverrides(user, effectivePermissionCodes, roles));
        return userRepository.save(user);
    }

    @Transactional
    public User update(UUID id, Boolean enabled, Set<String> roleCodes,
                       Set<String> effectivePermissionCodes, String rawPassword) {
        User user = findById(id);
        boolean wasAdmin = isAdmin(user.getRoles());
        if (enabled != null) user.setEnabled(enabled);
        if (roleCodes != null) {
            Set<Role> roles = resolveRoles(roleCodes);
            if (wasAdmin && !isAdmin(roles)) {
                throw new BusinessException("Không thể gỡ quyền Admin khỏi tài khoản quản trị", HttpStatus.FORBIDDEN);
            }
            user.setRoles(roles);
        }
        if (isAdmin(user.getRoles())) {
            user.getPermissionOverrides().clear();
        } else if (effectivePermissionCodes != null) {
            user.setPermissionOverrides(resolveOverrides(user, effectivePermissionCodes, user.getRoles()));
        }
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        return userRepository.save(user);
    }

    @Transactional
    public void delete(UUID id) {
        User user = findById(id);
        if (isAdmin(user.getRoles())) {
            throw new BusinessException("Không thể xóa tài khoản Admin", HttpStatus.FORBIDDEN);
        }
        userRepository.delete(user);
    }

    private Set<Role> resolveRoles(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) return new HashSet<>();
        return roleCodes.stream().map(code -> roleRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("Role not found: " + code, HttpStatus.BAD_REQUEST)))
                .collect(Collectors.toSet());
    }

    private Set<UserPermission> resolveOverrides(User user, Set<String> effectiveCodes, Set<Role> roles) {
        if (isAdmin(roles) || effectiveCodes == null) return new HashSet<>();
        if (effectiveCodes.contains("*")) {
            throw new BusinessException("Wildcard permission chỉ dành cho Admin", HttpStatus.FORBIDDEN);
        }
        List<Permission> permissions = permissionRepository.findAll();
        Set<String> known = permissions.stream().map(Permission::getCode).collect(Collectors.toSet());
        if (!known.containsAll(effectiveCodes)) {
            throw new BusinessException("Một hoặc nhiều permission không tồn tại", HttpStatus.BAD_REQUEST);
        }
        Set<String> inherited = roles.stream().flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode).collect(Collectors.toSet());
        Set<UserPermission> overrides = new HashSet<>();
        for (Permission permission : permissions) {
            String code = permission.getCode();
            if ("*".equals(code)) continue;
            boolean isInherited = inherited.contains(code);
            boolean isDesired = effectiveCodes.contains(code);
            if (isInherited != isDesired) {
                overrides.add(new UserPermission(user, permission, isDesired ? "GRANT" : "DENY"));
            }
        }
        return overrides;
    }

    private boolean isAdmin(Set<Role> roles) {
        return roles.stream().anyMatch(role -> "ADMIN".equals(role.getCode()));
    }
}
