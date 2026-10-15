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
        user.setRoles(new HashSet<>(roles));
        userRepository.save(user);
    }

    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    public List<Permission> listPermissions() {
        return permissionRepository.findAll();
    }
}
