package com.yms.yms.domain.user.service;

import com.yms.yms.domain.user.entity.Permission;
import com.yms.yms.domain.user.entity.Role;
import com.yms.yms.domain.user.entity.User;
import com.yms.yms.domain.user.entity.UserPermission;
import com.yms.yms.domain.user.repository.PermissionRepository;
import com.yms.yms.domain.user.repository.RoleRepository;
import com.yms.yms.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void updateMutatesManagedPermissionOverridesInsteadOfReplacingCollection() {
        UUID userId = UUID.randomUUID();
        Permission inheritedPermission = permission("FINANCE_READ");
        Permission overriddenPermission = permission("FINANCE_WRITE");
        Role staff = new Role();
        staff.setCode("STAFF");
        staff.setPermissions(new HashSet<>(Set.of(inheritedPermission, overriddenPermission)));

        User user = new User();
        user.setId(userId);
        user.setRoles(new HashSet<>(Set.of(staff)));
        UserPermission existingOverride = new UserPermission(user, overriddenPermission, "GRANT");
        user.getPermissionOverrides().add(existingOverride);
        Set<UserPermission> managedOverrides = user.getPermissionOverrides();

        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PermissionRepository permissions = mock(PermissionRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        when(permissions.findAll()).thenReturn(List.of(inheritedPermission, overriddenPermission));

        UserService service = new UserService(users, roles, passwordEncoder, permissions);
        User updated = service.update(userId, null, null, Set.of("FINANCE_READ"), null);

        assertSame(managedOverrides, updated.getPermissionOverrides());
        assertEquals(1, updated.getPermissionOverrides().size());
        assertEquals("DENY", existingOverride.getEffect());
        assertEquals(Set.of("FINANCE_READ"), UserPermissionResolver.effectiveCodes(updated));
    }

    @Test
    void updateAddsAndRemovesOverridesInManagedCollection() {
        UUID userId = UUID.randomUUID();
        Permission permission = permission("FINANCE_READ");
        User user = new User();
        user.setId(userId);
        Set<UserPermission> managedOverrides = user.getPermissionOverrides();
        managedOverrides.add(new UserPermission(user, permission, "DENY"));

        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PermissionRepository permissions = mock(PermissionRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        when(permissions.findAll()).thenReturn(List.of(permission));

        UserService service = new UserService(users, roles, passwordEncoder, permissions);
        service.update(userId, null, null, Set.of("FINANCE_READ"), null);

        assertSame(managedOverrides, user.getPermissionOverrides());
        assertEquals(1, managedOverrides.size());
        assertEquals("GRANT", managedOverrides.iterator().next().getEffect());

        service.update(userId, null, null, Set.of(), null);
        assertSame(managedOverrides, user.getPermissionOverrides());
        assertEquals(0, managedOverrides.size());
    }

    private static Permission permission(String code) {
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setCode(code);
        permission.setName(code);
        permission.setModule("FINANCE");
        return permission;
    }
}
