package com.yms.yms.domain.user.service;

import com.yms.yms.domain.user.entity.User;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class UserPermissionResolver {
    private UserPermissionResolver() { }

    public static Set<String> effectiveCodes(User user) {
        Set<String> effective = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .collect(Collectors.toCollection(HashSet::new));
        if (user.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getCode()))) {
            effective.add("*");
            return effective;
        }
        user.getPermissionOverrides().forEach(override -> {
            String code = override.getPermission().getCode();
            if ("DENY".equals(override.getEffect())) effective.remove(code);
            else effective.add(code);
        });
        return effective;
    }
}
