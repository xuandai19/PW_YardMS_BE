package com.yms.yms.domain.user.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.user.entity.Role;
import com.yms.yms.domain.user.entity.User;
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

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public User create(String username, String rawPassword, Set<String> roleCodes, boolean enabled) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Username already exists", HttpStatus.CONFLICT);
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEnabled(enabled);
        user.setRoles(resolveRoles(roleCodes));
        return userRepository.save(user);
    }

    @Transactional
    public User update(UUID id, Boolean enabled, Set<String> roleCodes, String rawPassword) {
        User user = findById(id);
        if (enabled != null) {
            user.setEnabled(enabled);
        }
        if (roleCodes != null) {
            user.setRoles(resolveRoles(roleCodes));
        }
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        return userRepository.save(user);
    }

    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException("User not found", HttpStatus.NOT_FOUND);
        }
        userRepository.deleteById(id);
    }

    private Set<Role> resolveRoles(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new HashSet<>();
        }
        return roleCodes.stream()
                .map(code -> roleRepository.findByCode(code)
                        .orElseThrow(() -> new BusinessException("Role not found: " + code, HttpStatus.BAD_REQUEST)))
                .collect(Collectors.toSet());
    }
}
